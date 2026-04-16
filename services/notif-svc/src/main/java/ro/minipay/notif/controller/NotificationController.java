package ro.minipay.notif.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.notif.model.Notification;
import ro.minipay.notif.service.NotificationService;

import java.util.List;
import java.util.Map;

/**
 * REST API for querying generated notifications.
 *
 * GET /notifications            — last 500 notifications (newest first)
 * GET /notifications/{txnId}   — notifications for a specific transaction
 * GET /notifications/stats      — summary counts by channel and status
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/{txnId}")
    public ResponseEntity<List<Notification>> getByTxnId(@PathVariable String txnId) {
        List<Notification> notifs = notificationService.getByTxnId(txnId);
        if (notifs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(notifs);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        List<Notification> all = notificationService.getAll();
        Map<String, Long> byChannel = all.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Notification::channel, java.util.stream.Collectors.counting()));
        Map<String, Long> byStatus = all.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Notification::paymentStatus, java.util.stream.Collectors.counting()));

        return ResponseEntity.ok(Map.of(
                "total",     all.size(),
                "byChannel", byChannel,
                "byPaymentStatus", byStatus
        ));
    }
}
