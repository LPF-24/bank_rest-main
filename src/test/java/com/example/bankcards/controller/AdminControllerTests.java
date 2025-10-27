package com.example.bankcards.controller;

import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.security.JWTUtil;
import com.example.bankcards.security.OwnerDetails;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired private JWTUtil jwtUtil;
    @Autowired private OwnerRepository ownerRepository;

    @AfterEach
    void clearDatabase() {
        ownerRepository.deleteAll();
    }

    @Nested
    class methodPromoteTests {
        @Test
        void promote_shouldChangeRole() throws Exception {
            Owner savedOwner = createSampleOwner("John", "Smith", "john23@gmail.com", Role.USER);
            Long userId = savedOwner.getId();
            String email = savedOwner.getEmail();

            String token = jwtUtil.generateAccessToken(userId, email, Role.USER.name());

            mockMvc.perform(patch("/admin/promote")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\": \"work2025admin\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content()
                            .string("You have been successfully promoted to administrator. Please log in again, colleague."));
        }

        @Test
        void promote_shouldFailValidation_whenCodeIsEmpty() throws Exception {
            Owner savedOwner = createSampleOwner("John", "Smith", "john23@gmail.com", Role.USER);
            Long userId = savedOwner.getId();
            String email = savedOwner.getEmail();

            String token = jwtUtil.generateAccessToken(userId, email, Role.USER.name());

            mockMvc.perform(patch("/admin/promote")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("code")))
                    .andExpect(jsonPath("$.path").value("/admin/promote"));
        }

        @Test
        void promote_shouldFailValidation_whenRoleIsAdmin() throws Exception {
            Owner savedOwner = createSampleOwner("John", "Smith", "john23@gmail.com", Role.ADMIN);
            Long userId = savedOwner.getId();
            String email = savedOwner.getEmail();

            String token = jwtUtil.generateAccessToken(userId, email, Role.ADMIN.name());

            mockMvc.perform(patch("/admin/promote")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\": \"work2025admin\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.path").value("/admin/promote"));
        }

        @Test
        void promote_shouldFail_whenUserNotFound() throws Exception {
            // Устанавливаем кастомный OwnerDetails с несуществующим id
            // Устанавливаем кастомный OwnerDetails с несуществующим id
            Owner fakeUser = new Owner();
            fakeUser.setId(9999L); // несуществующий ID
            fakeUser.setFirstName("John");
            fakeUser.setLastName("Smith");
            fakeUser.setDateOfBirth(LocalDate.of(2002, 3, 14));
            fakeUser.setEmail("john32@gmail.com");
            fakeUser.setPassword("secret");
            fakeUser.setPhone("+3-345-12-12");
            fakeUser.setRole(Role.USER);
            OwnerDetails fakeUserDetails = new OwnerDetails(fakeUser);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(fakeUserDetails, null, fakeUserDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);

            mockMvc.perform(patch("/admin/promote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\": \"work2025admin\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("error: Customer with this id 9999 can't be found"))
                    .andExpect(jsonPath("$.path").value("/admin/promote"));
        }
    }

    @Nested
    class methodGetAllCustomersTests {
        @AfterEach
        void clearDatabase() {
            ownerRepository.deleteAll();
        }

        @Test
        void getAllCustomers_shouldReceiveResponseDTOs() throws Exception {
            Owner savedOwner = createSampleOwner("John", "Smith", "john23@gmail.com", Role.ADMIN);
            Long userId = savedOwner.getId();
            String email = savedOwner.getEmail();

            String token = jwtUtil.generateAccessToken(userId, email, Role.ADMIN.name());
            createSampleOwner("Daniel", "Parker", "dan345@gmail.com", Role.USER);

            mockMvc.perform(get("/admin/all-customers")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].lastName").value("Parker"))
                    .andExpect(jsonPath("$[0].email").value("dan345@gmail.com"));
        }

        @Test
        void getAllCustomers_shouldReturnEmptyList_whenNoUsersExist() throws Exception {
            Owner savedOwner = createSampleOwner("John", "Smith", "john23@gmail.com", Role.ADMIN);
            Long userId = savedOwner.getId();
            String email = savedOwner.getEmail();

            String token = jwtUtil.generateAccessToken(userId, email, Role.ADMIN.name());

            mockMvc.perform(get("/admin/all-customers")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void getAllCustomers_shouldFailValidation_whenCodeIsEmpty() throws Exception {
            mockMvc.perform(get("/admin/all-customers"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Unauthorized: missing or invalid token"))
                    .andExpect(jsonPath("$.path").value("/admin/all-customers"));
        }

        @Test
        void getAllCustomers_shouldFailMethod_whenRoleIsUser() throws Exception {
            Owner savedOwner = createSampleOwner("John", "Smith", "john23@gmail.com", Role.USER);
            Long userId = savedOwner.getId();
            String email = savedOwner.getEmail();

            String token = jwtUtil.generateAccessToken(userId, email, Role.USER.name());

            mockMvc.perform(get("/admin/all-customers")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.path").value("/admin/all-customers"));
        }

        @Test
        void getAllCustomers_shouldReturnUnauthorized_whenTokenIsInvalid() throws Exception {
            String token = "Bearer invalid.token.value";

            mockMvc.perform(get("/admin/all-customers")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    @Nested
    class methodBlockCustomerTests {

        @AfterEach
        void cleanup() {
            ownerRepository.deleteAll();
        }

        @Test
        void blockCustomer_shouldReturn200_forAdmin() throws Exception {
            Owner admin = createSampleOwner("Admin", "Boss", "admin@example.com", Role.ADMIN);
            Owner target = createSampleOwner("John", "Smith", "john23@gmail.com", Role.USER);

            String adminToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), Role.ADMIN.name());

            mockMvc.perform(patch("/admin/block-customer/{id}", target.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Customer's account with id " + target.getId() + " is locked."));
        }

        @Test
        void blockCustomer_shouldReturn403_forUser() throws Exception {
            Owner user = createSampleOwner("User", "U", "user@example.com", Role.USER);
            Owner target = createSampleOwner("Daniel", "Parker", "dan345@gmail.com", Role.USER);

            String userToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), Role.USER.name());

            mockMvc.perform(patch("/admin/block-customer/{id}", target.getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.path").value("/admin/block-customer/" + target.getId()));
        }

        @Test
        void blockCustomer_shouldReturn401_whenNoToken() throws Exception {
            mockMvc.perform(patch("/admin/block-customer/{id}", 123L))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.path").value("/admin/block-customer/123"));
        }

        @Test
        void blockCustomer_shouldReturn404_whenTargetNotFound() throws Exception {
            Owner admin = createSampleOwner("Admin", "Boss", "admin@example.com", Role.ADMIN);
            String adminToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), Role.ADMIN.name());

            long missingId = 999_999L;

            mockMvc.perform(patch("/admin/block-customer/{id}", missingId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.path").value("/admin/block-customer/" + missingId));
        }
    }

    private Owner createSampleOwner(String firstName, String lastName, String email, Role role) {
        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setDateOfBirth(LocalDate.of(2002, 3, 14));
        owner.setEmail(email);
        owner.setPassword("secret");
        owner.setPhone("+3-345-12-12");
        owner.setRole(role);
        return ownerRepository.save(owner);
    }
}
