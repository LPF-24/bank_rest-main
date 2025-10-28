package com.example.bankcards.controller;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Currency;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class CardControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private OwnerRepository ownerRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        cardRepository.deleteAll();
        ownerRepository.deleteAll();
    }

    @Test
    void getMyCards_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/cards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyCards_shouldReturn200_andOnlyCurrentUserCards_sortedByCreatedAtDesc() throws Exception {
        Owner user = createOwner("user@example.com", Role.USER);
        Owner other = createOwner("other@example.com", Role.USER);

        // для user: две карты с разными createdAt
        Card newer = createCard(user, "1111", LocalDateTime.now().plusMinutes(1), CardStatus.ACTIVE);
        Card older = createCard(user, "2222", LocalDateTime.now().minusMinutes(10), CardStatus.BLOCKED);
        // чужая карта
        createCard(other, "9999", LocalDateTime.now(), CardStatus.ACTIVE);

        String token = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), "USER");

        mockMvc.perform(get("/cards")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // проверяем страницу spring-data: content, totalElements, сортировка по createdAt desc
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                // первый — самый новый (newer)
                .andExpect(jsonPath("$.content[0].id", is(newer.getId().intValue())))
                .andExpect(jsonPath("$.content[0].maskedPan", endsWith("1111")))
                .andExpect(jsonPath("$.content[0].status", is("ACTIVE")))
                // второй — старее (older)
                .andExpect(jsonPath("$.content[1].id", is(older.getId().intValue())))
                .andExpect(jsonPath("$.content[1].maskedPan", endsWith("2222")))
                .andExpect(jsonPath("$.content[1].status", is("BLOCKED")));
    }

    @Test
    void getMyCards_shouldSupportPagination() throws Exception {
        Owner user = createOwner("pag@example.com", Role.USER);

        Card newer = createCard(user, "5555", LocalDateTime.now().plusMinutes(5), CardStatus.ACTIVE);
        Card mid   = createCard(user, "4444", LocalDateTime.now(),               CardStatus.ACTIVE);
        Card older = createCard(user, "3333", LocalDateTime.now().minusMinutes(5), CardStatus.BLOCKED);

        String token = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), "USER");

        // page=0,size=1 -> только newest
        mockMvc.perform(get("/cards?page=0&size=1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(newer.getId().intValue())))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(1)));

        // page=1,size=1 -> второй по новизне (mid)
        mockMvc.perform(get("/cards?page=1&size=1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(mid.getId().intValue())))
                .andExpect(jsonPath("$.number", is(1)))
                .andExpect(jsonPath("$.size", is(1)));
    }

    // --- helpers ---
    private Owner createOwner(String email, Role role) {
        Owner o = new Owner();
        o.setFirstName("John");
        o.setLastName("Smith");
        o.setDateOfBirth(LocalDate.of(1990, 1, 1));
        o.setEmail(email);
        o.setPassword("secret");
        o.setPhone("+1000000");
        o.setRole(role);
        o.setLocked(false);
        return ownerRepository.save(o);
    }

    private Card createCard(Owner owner, String last4, LocalDateTime createdAt, CardStatus status) {
        Card c = new Card();
        c.setOwner(owner);
        c.setPan("stub");                 // pan_encrypted NOT NULL обеспечим через конвертер
        c.setPanLast4(last4);
        c.setBin("400000");               // ✅ ОБЯЗАТЕЛЬНО: BIN not null
        c.setExpiryMonth((short) 10);
        c.setExpiryYear((short) 2030);
        c.setStatus(status);
        c.setBalance(BigDecimal.ZERO);
        c.setCurrency(Currency.USD);

        c = cardRepository.save(c);

        // created_at у тебя insertable=false, Liquibase выключен → БД не ставит дефолт.
        // Проставим вручную через SQL, чтобы сортировка по created_at работала.
        // убери import java.sql.Timestamp;

        jdbcTemplate.update(
                "UPDATE card SET created_at = ? WHERE id = ?",
                createdAt,           // <-- LocalDateTime
                c.getId()
        );

        return c;
    }
}
