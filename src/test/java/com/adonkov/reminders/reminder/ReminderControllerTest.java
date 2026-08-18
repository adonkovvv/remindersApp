package com.adonkov.reminders.reminder;

import com.adonkov.reminders.common.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReminderController.class)
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReminderService service;

    private static ReminderDtos.Response sample(long id) {
        Instant due = Instant.now().plus(1, ChronoUnit.DAYS);
        return new ReminderDtos.Response(id, "Pay rent", "before the 5th", due, false, Instant.now(), Instant.now());
    }

    @Test
    void listsReminders() throws Exception {
        given(service.search(any(), any(), any(), any()))
                .willReturn(new PageResponse<>(List.of(sample(1L)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Pay rent"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returns404WhenMissing() throws Exception {
        willThrow(new ReminderNotFoundException(99L)).given(service).findById(eq(99L));

        mockMvc.perform(get("/api/reminders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Reminder not found"));
    }

    @Test
    void createsReminder() throws Exception {
        Instant due = Instant.now().plus(2, ChronoUnit.DAYS);
        given(service.create(any())).willReturn(sample(5L));

        var body = new ReminderDtos.CreateRequest("Pay rent", "before the 5th", due);

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        var body = new ReminderDtos.CreateRequest("  ", null, Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void rejectsDueDateInThePast() throws Exception {
        var body = new ReminderDtos.CreateRequest("Pay rent", null, Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dueAt").exists());
    }
}
