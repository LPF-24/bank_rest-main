package com.example.bankcards.controller;

import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.security.JWTUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class AdminCardControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private OwnerRepository ownerRepository;
    @Autowired private JWTUtil jwtUtil;
    @Autowired private CardRepository cardRepository;

    @AfterEach
    void cleanup() {
        cardRepository.deleteAll();   // сначала дочерние
        ownerRepository.deleteAll();  // потом родители
    }

    private Owner createOwner(String email, Role role) {
        Owner o = new Owner();
        o.setFirstName("John");
        o.setLastName("Smith");
        o.setDateOfBirth(LocalDate.of(2000,1,1));
        o.setEmail(email);
        o.setPassword("secret");
        o.setPhone("+1000000");
        o.setRole(role);
        o.setLocked(false);
        return ownerRepository.save(o);
    }

    @Test
    void create_shouldReturn201_forAdmin() throws Exception {
        Owner admin = createOwner("admin@example.com", Role.ADMIN);
        Owner target = createOwner("user@example.com", Role.USER);

        String token = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

        mockMvc.perform(post("/admin/cards")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerId\": " + target.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(target.getId()))
                .andExpect(jsonPath("$.maskedPan").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_shouldReturn403_forUser() throws Exception {
        Owner user = createOwner("user@example.com", Role.USER);
        Owner target = createOwner("target@example.com", Role.USER);

        String token = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), "USER");

        mockMvc.perform(post("/admin/cards")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerId\": " + target.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldReturn404_ifOwnerMissing() throws Exception {
        Owner admin = createOwner("admin2@example.com", Role.ADMIN);
        String token = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

        mockMvc.perform(post("/admin/cards")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerId\": 999999}"))
                .andExpect(status().isNotFound());
    }
}
