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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Nested
    class AdminFindCardsIntegrationTests {

        @Test
        void findAll_shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/cards"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void findAll_shouldReturn403_whenUserRole() throws Exception {
            Owner user = createOwner("user@example.com", Role.USER);
            String token = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(get("/admin/cards")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void findAll_shouldReturn200_andSortedByCreatedAtDesc_forAdmin() throws Exception {
            Owner admin = createOwner("admin@example.com", Role.ADMIN);
            Owner a = createOwner("a@example.com", Role.USER);

            // 3 карты одному пользователю с разными created_at
            Card newest = createCard(a, "1111", LocalDateTime.now().plusMinutes(2), CardStatus.ACTIVE);
            Card mid    = createCard(a, "2222", LocalDateTime.now(),               CardStatus.BLOCKED);
            Card oldest = createCard(a, "3333", LocalDateTime.now().minusMinutes(5), CardStatus.ACTIVE);

            String token = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(get("/admin/cards")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(newest.getId().intValue()))
                    .andExpect(jsonPath("$.content[1].id").value(mid.getId().intValue()))
                    .andExpect(jsonPath("$.content[2].id").value(oldest.getId().intValue()))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        void findAll_shouldFilterByOwnerId() throws Exception {
            Owner admin = createOwner("admin2@example.com", Role.ADMIN);
            Owner o1 = createOwner("u1@example.com", Role.USER);
            Owner o2 = createOwner("u2@example.com", Role.USER);

            createCard(o1, "1111", LocalDateTime.now(), CardStatus.ACTIVE);
            createCard(o2, "2222", LocalDateTime.now(), CardStatus.BLOCKED);

            String token = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(get("/admin/cards")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .param("ownerId", o1.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].ownerId").value(o1.getId()))
                    .andExpect(jsonPath("$.content[0].maskedPan").value("**** **** **** 1111"));
        }

        @Test
        void findAll_shouldFilterByEmailStatusBinAndLast4() throws Exception {
            Owner admin = createOwner("admin3@example.com", Role.ADMIN);
            Owner u = createOwner("filter@example.com", Role.USER);

            createCard(u, "4444", LocalDateTime.now(), CardStatus.ACTIVE);
            createCard(u, "5555", LocalDateTime.now(), CardStatus.BLOCKED); // ожидаем найти эту

            String token = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(get("/admin/cards")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .param("email", "filter@example.com")
                            .param("status", "BLOCKED")
                            .param("bin", "400000")
                            .param("panLast4", "5555"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].status").value("BLOCKED"))
                    .andExpect(jsonPath("$.content[0].maskedPan").value("**** **** **** 5555"));
        }

        @Test
        void findAll_shouldSupportPagination() throws Exception {
            Owner admin = createOwner("admin4@example.com", Role.ADMIN);
            Owner u = createOwner("pag@example.com", Role.USER);

            Card c1 = createCard(u, "1001", LocalDateTime.now().plusMinutes(3), CardStatus.ACTIVE);
            Card c2 = createCard(u, "1002", LocalDateTime.now().plusMinutes(2), CardStatus.ACTIVE);
            Card c3 = createCard(u, "1003", LocalDateTime.now().plusMinutes(1), CardStatus.ACTIVE);

            String token = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(get("/admin/cards")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(c1.getId().intValue()));

            mockMvc.perform(get("/admin/cards")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .param("page", "1")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(c3.getId().intValue()));
        }
    }

    @Nested
    class AdminDeleteCardIT {

        @Test
        void delete_shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(delete("/admin/cards/{id}", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void delete_shouldReturn403_whenUserIsNotAdmin() throws Exception {
            Owner user = createOwner("user@example.com", Role.USER);
            String token = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(delete("/admin/cards/{id}", 1L)
                            .with(csrf())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_shouldReturn404_whenCardNotFound() throws Exception {
            Owner admin = createOwner("admin@example.com", Role.ADMIN);
            String token = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(delete("/admin/cards/{id}", 999999L)
                            .with(csrf())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        void delete_shouldReturn204_whenAdminDeletesExisting() throws Exception {
            Owner admin = createOwner("okadmin@example.com", Role.ADMIN);
            Owner user  = createOwner("cardowner@example.com", Role.USER);
            Card  card  = createCard(user, "1111", LocalDateTime.now(), CardStatus.ACTIVE);

            String token = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(delete("/admin/cards/{id}", card.getId())
                            .with(csrf())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNoContent());

            // Дополнительно убедимся, что карты больше нет в БД
            assertTrue(cardRepository.findById(card.getId()).isEmpty());
        }

        @Test
        void delete_shouldReturn400_whenIdIsNotNumber() throws Exception {
            Owner admin = createOwner("badid-admin@example.com", Role.ADMIN);
            String token = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(delete("/admin/cards/{id}", "abc")
                            .with(csrf())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isBadRequest());
            // тело ошибки (если хочешь): .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"))
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
