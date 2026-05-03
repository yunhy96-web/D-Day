package com.hauly.intake.order.application;

import com.hauly.intake.order.application.command.CreateOrderNoteCommand;
import com.hauly.intake.order.application.command.UpdateOrderNoteCommand;
import com.hauly.intake.order.application.query.OrderNoteView;
import com.hauly.intake.order.domain.model.OrderNote;
import com.hauly.intake.order.domain.repository.OrderNoteRepository;
import com.hauly.intake.order.domain.repository.OrderRepository;
import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.repository.AppUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service for order notes — collaborative comments attached to an order.
 * Edit and delete are restricted to the original author. Deletion is soft so the
 * audit trail survives.
 *
 * Mutating operations cross-validate that the addressed note actually belongs to the
 * orderId in the request path; this keeps REST resource semantics intact and prevents
 * a user from acting on their own notes through an unrelated order's URL.
 */
@Service
@Transactional
public class OrderNoteService {

    private static final int MAX_BODY_LENGTH = 2000;

    private final OrderRepository orderRepository;
    private final OrderNoteRepository noteRepository;
    private final AppUserRepository appUserRepository;

    public OrderNoteService(OrderRepository orderRepository,
                            OrderNoteRepository noteRepository,
                            AppUserRepository appUserRepository) {
        this.orderRepository = orderRepository;
        this.noteRepository = noteRepository;
        this.appUserRepository = appUserRepository;
    }

    public OrderNoteView create(CreateOrderNoteCommand cmd, Long authorId) {
        String body = normalizeBody(cmd.body());
        // Existence check first so deleting the order ID returns 404 instead of orphaning a note.
        orderRepository.findById(cmd.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + cmd.orderId()));

        OrderNote note = OrderNote.create(cmd.orderId(), authorId, body);
        OrderNote saved = noteRepository.save(note);
        return toView(saved);
    }

    public OrderNoteView update(UpdateOrderNoteCommand cmd, Long actorId) {
        String body = normalizeBody(cmd.body());
        OrderNote note = loadActiveForOrder(cmd.orderId(), cmd.noteId());
        requireAuthor(note, actorId);
        note.edit(body);
        // Explicit save: dirty checking would suffice today, but flushing here keeps the
        // contract clear if @Transactional boundaries change later.
        OrderNote saved = noteRepository.save(note);
        return toView(saved);
    }

    public void delete(Long orderId, Long noteId, Long actorId) {
        OrderNote note = loadActiveForOrder(orderId, noteId);
        requireAuthor(note, actorId);
        note.softDelete();
        noteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public List<OrderNoteView> listByOrder(Long orderId) {
        List<OrderNote> notes = noteRepository.findActiveByOrderId(orderId);
        Map<Long, String> authorNames = resolveAuthorNames(notes);
        return notes.stream()
                .map(n -> OrderNoteView.from(n, authorNames.getOrDefault(n.getAuthorId(), "")))
                .toList();
    }

    private OrderNote loadActiveForOrder(Long orderId, Long noteId) {
        OrderNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));
        if (note.isDeleted() || !note.getOrderId().equals(orderId)) {
            // Treat "wrong order" identically to "not found" — don't leak whether the
            // note exists under a different order.
            throw new IllegalArgumentException("Note not found: " + noteId);
        }
        return note;
    }

    private static void requireAuthor(OrderNote note, Long actorId) {
        if (!note.isAuthor(actorId)) {
            throw new AccessDeniedException("Only the author can modify this note");
        }
    }

    private static String normalizeBody(String raw) {
        if (raw == null) throw new IllegalArgumentException("note body is required");
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("note body is required");
        if (trimmed.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("note body too long (max " + MAX_BODY_LENGTH + ")");
        }
        return trimmed;
    }

    private OrderNoteView toView(OrderNote note) {
        String name = appUserRepository.findById(note.getAuthorId())
                .map(OrderNoteService::displayLabel)
                .orElse("");
        return OrderNoteView.from(note, name);
    }

    /** Single batch query — listing N notes resolves k unique authors in one round trip. */
    private Map<Long, String> resolveAuthorNames(List<OrderNote> notes) {
        Set<Long> authorIds = notes.stream()
                .map(OrderNote::getAuthorId)
                .collect(Collectors.toSet());
        if (authorIds.isEmpty()) return Map.of();
        Map<Long, String> result = new HashMap<>();
        for (AppUser user : appUserRepository.findAllById(authorIds)) {
            result.put(user.getId(), displayLabel(user));
        }
        return result;
    }

    /** Prefer display name; fall back to username so the UI never shows a blank author. */
    private static String displayLabel(AppUser user) {
        String dn = user.getDisplayName();
        if (dn != null && !dn.isBlank()) return dn;
        String username = user.getUsernameValue();
        return username == null ? "" : username;
    }
}
