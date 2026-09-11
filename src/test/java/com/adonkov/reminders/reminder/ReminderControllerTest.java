package com.adonkov.reminders.reminder;

import com.adonkov.reminders.auth.AuthenticatedUser;
import com.adonkov.reminders.common.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReminderController.class)
class ReminderControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReminderService service;

    private static RequestPostProcessor asUser() {
        var principal = new AuthenticatedUser(USER_ID, "ivan@example.com");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static ReminderDtos.Response sample(long id) {
        Instant due = Instant.now().plus(1, ChronoUnit.DAYS);
        return new ReminderDtos.Response(id, "Pay rent", "before the 5th", due, false,
                Recurrence.NONE, null, Instant.now(), Instant.now());
    }

    @Test
    void listsReminders() throws Exception {
        given(service.search(eq(USER_ID), any(), any(), any(), any()))
                .willReturn(new PageResponse<>(List.of(sample(1L)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/reminders").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Pay rent"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns404WhenMissing() throws Exception {
        willThrow(new ReminderNotFoundException(99L)).given(service).findById(eq(USER_ID), eq(99L));

        mockMvc.perform(get("/api/reminders/99").with(asUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Reminder not found"));
    }

    @Test
    void createsReminder() throws Exception {
        Instant due = Instant.now().plus(2, ChronoUnit.DAYS);
        given(service.create(eq(USER_ID), any())).willReturn(sample(5L));

        var body = new ReminderDtos.CreateRequest("Pay rent", "before the 5th", due, Recurrence.DAILY);

        mockMvc.perform(post("/api/reminders").with(asUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        var body = new ReminderDtos.CreateRequest("  ", null, Instant.now().plus(1, ChronoUnit.DAYS), null);

        mockMvc.perform(post("/api/reminders").with(asUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void rejectsDueDateInThePast() throws Exception {
        var body = new ReminderDtos.CreateRequest("Pay rent", null, Instant.now().minus(1, ChronoUnit.DAYS), null);

        mockMvc.perform(post("/api/reminders").with(asUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dueAt").exists());
    }
}
