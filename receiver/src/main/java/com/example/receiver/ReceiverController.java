package com.example.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/hooks")
public class ReceiverController {
    private static final Logger log = LoggerFactory.getLogger(ReceiverController.class);

    private final JavaMailSender mailSender;
    private final String emailTo;

    private final List<Map<String, Object>> received = new CopyOnWriteArrayList<>();

    public ReceiverController(ObjectProvider<JavaMailSender> mailSenderProvider,
                              @Value("${receiver.email.to:}") String emailTo) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.emailTo = emailTo == null ? "" : emailTo.trim();
    }

    @PostMapping("/process")
    public ResponseEntity<String> receive(@RequestBody Map<String, Object> payload) {
        Map<String, Object> entry = Map.of(
                "receivedAt", Instant.now().toString(),
                "payload", payload
        );
        received.add(entry);
        log.info("Received webhook payload: {}", payload);

        // If an email recipient is configured, send a simple notification email.
        if (emailTo.length() > 0 && mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(emailTo);
                msg.setSubject("Cron Receiver: payload received");
                msg.setText("Received payload at " + entry.get("receivedAt") + "\n\nPayload:\n" + payload.toString());
                mailSender.send(msg);
                log.info("Notification email sent to {}", emailTo);
            } catch (Exception ex) {
                log.error("Failed to send notification email", ex);
            }
        } else if (emailTo.length() > 0) {
            log.warn("Email recipient configured but no JavaMailSender available; skipping email");
        }

        return ResponseEntity.ok("received");
    }

    @GetMapping("/received")
    public List<Map<String, Object>> list() {
        return Collections.unmodifiableList(received);
    }
}
