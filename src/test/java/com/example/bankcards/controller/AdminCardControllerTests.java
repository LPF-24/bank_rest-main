package com.example.bankcards.controller;

import com.example.bankcards.entity.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.security.JWTUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class AdminCardControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private OwnerRepository ownerRepository;
    @Autowired private JWTUtil jwtUtil;
    @Autowired private CardRepository cardRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

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

    @Nested
    class BlockAndUnblockCardByAdminTests {
        @Test
        void adminBlock_shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(patch("/admin/cards/{id}/block", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void adminUnblock_shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(patch("/admin/cards/{id}/unblock", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void adminBlock_shouldReturn403_whenUserIsNotAdmin() throws Exception {
            Owner user = createOwner("user@example.com", Role.USER);
            String userToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(patch("/admin/cards/{id}/block", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminUnblock_shouldReturn403_whenUserIsNotAdmin() throws Exception {
            Owner user = createOwner("user2@example.com", Role.USER);
            String userToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(patch("/admin/cards/{id}/unblock", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminBlock_shouldReturn404_whenCardNotFound() throws Exception {
            Owner admin = createOwner("admin@example.com", Role.ADMIN);
            String adminToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(patch("/admin/cards/{id}/block", 999999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        void adminUnblock_shouldReturn404_whenCardNotFound() throws Exception {
            Owner admin = createOwner("admin2@example.com", Role.ADMIN);
            String adminToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(patch("/admin/cards/{id}/unblock", 999999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        void adminBlock_shouldReturn200_andBlocked_whenAdmin() throws Exception {
            Owner admin = createOwner("okadmin@example.com", Role.ADMIN);
            Owner user  = createOwner("userx@example.com", Role.USER);
            Card card  = createCard(user, "1111", LocalDateTime.now(), CardStatus.ACTIVE);

            String adminToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(patch("/admin/cards/{id}/block", card.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(card.getId().intValue()))
                    .andExpect(jsonPath("$.status").value("BLOCKED"));
        }

        @Test
        void adminUnblock_shouldReturn200_andActive_whenAdmin() throws Exception {
            Owner admin = createOwner("okadmin2@example.com", Role.ADMIN);
            Owner user  = createOwner("usery@example.com", Role.USER);
            Card  card  = createCard(user, "2222", LocalDateTime.now(), CardStatus.BLOCKED);

            String adminToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(patch("/admin/cards/{id}/unblock", card.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(card.getId().intValue()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void adminBlock_shouldBeIdempotent_whenAlreadyBlocked() throws Exception {
            Owner admin = createOwner("idemadmin@example.com", Role.ADMIN);
            Owner user  = createOwner("userz@example.com", Role.USER);
            Card  card  = createCard(user, "3333", LocalDateTime.now(), CardStatus.BLOCKED);

            String adminToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(patch("/admin/cards/{id}/block", card.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("BLOCKED"));
        }

        @Test
        void adminUnblock_shouldBeIdempotent_whenAlreadyActive() throws Exception {
            Owner admin = createOwner("idemadmin2@example.com", Role.ADMIN);
            Owner user  = createOwner("userw@example.com", Role.USER);
            Card  card  = createCard(user, "4444", LocalDateTime.now(), CardStatus.ACTIVE);

            String adminToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(patch("/admin/cards/{id}/unblock", card.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
    }

    private Card createCard(Owner owner, String last4, LocalDateTime createdAt, CardStatus status) {
        Card card = new Card();
        card.setOwner(owner);
        card.setPan("stub");               // поле pan_encrypted через конвертер
        card.setPanLast4(last4);
        card.setBin("400000");             // обязательное поле: nullable = false
        card.setExpiryMonth((short)10);
        card.setExpiryYear((short)2030);
        card.setStatus(status);
        card.setBalance(BigDecimal.ZERO);
        card.setCurrency(Currency.USD);

        Card saved = cardRepository.save(card);

        // вручную проставляем created_at, т.к. оно insertable=false (не вставляется через JPA)
        jdbcTemplate.update(
                "UPDATE card SET created_at = ? WHERE id = ?",
                createdAt,
                saved.getId()
        );

        return saved;
    }
}
