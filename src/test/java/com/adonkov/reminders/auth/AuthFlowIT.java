package com.adonkov.reminders.auth;

import com.adonkov.reminders.AbstractPostgresTest;
import com.adonkov.reminders.reminder.ReminderRepository;
import com.adonkov.reminders.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIT extends AbstractPostgresTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReminderRepository reminders;

    @Autowired
    private UserRepository users;

    @BeforeEach
    void clean() {
        reminders.deleteAll();
        users.deleteAll();
    }

    private String register(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "hunter2hunter2",
                "displayName", "Test"));

        String json = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("token").asText();
    }

    private long createReminder(String token, String title) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "dueAt", Instant.now().plus(1, ChronoUnit.DAYS).toString()));

        String json = mockMvc.perform(post("/api/reminders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }

    @Test
    void registerThenUseTheTokenToCreateAndReadReminders() throws Exception {
        String token = register("ivan@example.com");
        long id = createReminder(token, "pay rent");

        mockMvc.perform(get("/api/reminders/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("pay rent"));
    }

    @Test
    void oneUsersRemindersAreInvisibleToAnother() throws Exception {
        String ivan = register("ivan@example.com");
        long id = createReminder(ivan, "pay rent");

        String maria = register("maria@example.com");

        mockMvc.perform(get("/api/reminders/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + maria))
                .andExpect(status().isNotFound());

        String listing = mockMvc.perform(get("/api/reminders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + maria))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode content = objectMapper.readTree(listing).get("content");
        assertThat(content).isEmpty();
    }

    @Test
    void registeringTheSameEmailTwiceIsRejected() throws Exception {
        register("ivan@example.com");

        String duplicate = objectMapper.writeValueAsString(Map.of(
                "email", "IVAN@example.com",
                "password", "hunter2hunter2"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicate))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        register("ivan@example.com");

        String login = objectMapper.writeValueAsString(Map.of(
                "email", "ivan@example.com",
                "password", "notmypassword"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/reminders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }
}
