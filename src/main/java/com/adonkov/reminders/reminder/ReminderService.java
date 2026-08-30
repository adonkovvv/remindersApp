package com.adonkov.reminders.reminder;

import com.adonkov.reminders.common.PageResponse;
import com.adonkov.reminders.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReminderService {

    private final ReminderRepository repository;
    private final UserRepository users;

    public ReminderService(ReminderRepository repository, UserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReminderDtos.Response> search(Long userId,
                                                      Boolean completed,
                                                      Instant dueBefore,
                                                      Instant dueAfter,
                                                      Pageable pageable) {
        Specification<Reminder> spec = Specification
                .allOf(ReminderSpecifications.ownedBy(userId),
                        ReminderSpecifications.completedIs(completed),
                        ReminderSpecifications.dueBefore(dueBefore),
                        ReminderSpecifications.dueAfter(dueAfter));

        Page<Reminder> page = repository.findAll(spec, pageable);
        return PageResponse.of(page, ReminderDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public ReminderDtos.Response findById(Long userId, Long id) {
        return ReminderDtos.Response.from(load(userId, id));
    }

    @Transactional
    public ReminderDtos.Response create(Long userId, ReminderDtos.CreateRequest request) {
        Reminder reminder = new Reminder(
                users.getReferenceById(userId),
                request.title(),
                request.description(),
                request.dueAt());

        return ReminderDtos.Response.from(repository.save(reminder));
    }

    @Transactional
    public ReminderDtos.Response update(Long userId, Long id, ReminderDtos.UpdateRequest request) {
        Reminder reminder = load(userId, id);
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
    public ReminderDtos.Response complete(Long userId, Long id) {
        Reminder reminder = load(userId, id);
        reminder.setCompleted(true);
        return ReminderDtos.Response.from(reminder);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(load(userId, id));
    }

    /**
     * Reminders owned by someone else are reported as missing rather than forbidden,
     * so the API doesn't leak which ids exist.
     */
    private Reminder load(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ReminderNotFoundException(id));
    }
}
