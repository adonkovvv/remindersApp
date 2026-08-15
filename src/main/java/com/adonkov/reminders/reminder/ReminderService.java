package com.adonkov.reminders.reminder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReminderService {

    private final ReminderRepository repository;

    public ReminderService(ReminderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ReminderDtos.Response> findAll() {
        return repository.findAll().stream()
                .map(ReminderDtos.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReminderDtos.Response findById(Long id) {
        return ReminderDtos.Response.from(load(id));
    }

    @Transactional
    public ReminderDtos.Response create(ReminderDtos.CreateRequest request) {
        Reminder reminder = new Reminder(request.title(), request.description(), request.dueAt());
        return ReminderDtos.Response.from(repository.save(reminder));
    }

    @Transactional
    public ReminderDtos.Response update(Long id, ReminderDtos.UpdateRequest request) {
        Reminder reminder = load(id);
        if (request.title() != null) {
            reminder.setTitle(request.title());
        }
        if (request.description() != null) {
            reminder.setDescription(request.description());
        }
        if (request.dueAt() != null) {
            reminder.setDueAt(request.dueAt());
        }
        if (request.completed() != null) {
            reminder.setCompleted(request.completed());
        }
        return ReminderDtos.Response.from(reminder);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(load(id));
    }

    private Reminder load(Long id) {
        return repository.findById(id).orElseThrow(() -> new ReminderNotFoundException(id));
    }
}
