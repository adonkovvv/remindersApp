package com.adonkov.reminders.reminder;

import com.adonkov.reminders.auth.AuthenticatedUser;
import com.adonkov.reminders.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService service;

    public ReminderController(ReminderService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ReminderDtos.Response> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueAfter,
            @PageableDefault(size = 20, sort = "dueAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(user.id(), completed, dueBefore, dueAfter, pageable);
    }

    @GetMapping("/{id}")
    public ReminderDtos.Response get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return service.findById(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderDtos.Response create(@AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody ReminderDtos.CreateRequest request) {
        return service.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public ReminderDtos.Response update(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable Long id,
                                        @Valid @RequestBody ReminderDtos.UpdateRequest request) {
        return service.update(user.id(), id, request);
    }

    @PatchMapping("/{id}/complete")
    public ReminderDtos.Response complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return service.complete(user.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
    }
}
