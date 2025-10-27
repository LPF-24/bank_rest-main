package com.example.bankcards.controller;

import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.security.JWTUtil;
import com.example.bankcards.security.OwnerDetails;
import com.example.bankcards.security.OwnerDetailsService;
import com.example.bankcards.service.OwnerService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OwnerControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired private OwnerRepository ownerRepository;
    @Autowired private OwnerService ownerService;
    //@Autowired private OwnerValidator ownerValidator; <-- возможно, на будущее сделаем
    @Autowired private JWTUtil jwtUtil;

    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private OwnerDetailsService ownerDetailsService;

    @AfterEach
    void clearDatabase() {
        ownerRepository.deleteAll();
    }

    @Nested
    class performAuthenticationTests {

        @BeforeEach
        void setUp() {
            Owner owner = createSampleOwner("John", "Smith", "john23@gmail.com", Role.USER);
            Owner saved = ownerRepository.save(owner);

            OwnerDetails personDetails = new OwnerDetails(saved);

            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken(personDetails, null,
                            personDetails.getAuthorities()));
        }

        @Test
        void performAuthentication_success() throws Exception {
            mockMvc.perform(post("/owner/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "john23@gmail.com",
                                        "password": "secret"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john23@gmail.com"));
        }

        @Test
        void performAuthentication_shouldThrowUnauthorizeException() throws Exception {
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            mockMvc.perform(post("/owner/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "username": "user123",
                                        "password": "password"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"))
                    .andExpect(jsonPath("$.path").value("/owner/login"));
        }
    }

    @Nested
    class registrationTests {
        @Test
        void register_success() throws Exception {
            mockMvc.perform(post("/owner/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "John",
                                      "lastName": "Smith",
                                      "dateOfBirth": "2000-12-12",
                                      "email": "john23@gmail.com",
                                      "phone": "+33451212",
                                      "password": "Test234!"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.email").value("john23@gmail.com"))
                    .andExpect(jsonPath("$.role").value(Role.USER.name()));
        }

        @Test
        void register_shouldThrowsValidationException_whenDataIsNotCorrect() throws Exception {
            mockMvc.perform(post("/owner/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "J",
                                      "lastName": "S",
                                      "email": "john23gmail.com",
                                      "phone": "33451212",
                                      "password": "test234"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", Matchers.allOf(
                            containsString("firstName:"),
                            containsString("lastName:"),
                            containsString("email:"),
                            containsString("phone:"),
                            containsString("password:"))));
        }

        @Test
        void register_shouldThrowsException_whenUserWithThisEmailIsAlreadyRegistered() throws Exception {
            ownerRepository.save(createSampleOwner("John", "Smith", "john23@gmail.com", Role.USER));

            mockMvc.perform(post("/owner/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "John",
                                      "lastName": "Smith",
                                      "dateOfBirth": "2000-12-12",
                                      "email": "john23@gmail.com",
                                      "phone": "+33451212",
                                      "password": "Test234!"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("This email is already taken!")));
        }
    }

    private static Owner createSampleOwner(String firstName, String lastName, String email, Role role) {
        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setDateOfBirth(LocalDate.of(2002, 3, 14));
        owner.setEmail(email);
        owner.setPassword("secret");
        owner.setPhone("+3-345-12-12");
        owner.setRole(role);
        return owner;
    }

    private static OwnerResponseDTO createSampleResponseDTO() {
        OwnerResponseDTO response = new OwnerResponseDTO();
        response.setId(1L);
        response.setFirstName("John");
        response.setLastName("Smith");
        response.setEmail("john@gmail.com");
        response.setRole(Role.USER);
        return response;
    }
}
