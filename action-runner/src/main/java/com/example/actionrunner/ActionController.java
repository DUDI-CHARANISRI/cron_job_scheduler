package com.example.actionrunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/execute")
public class ActionController {
    private static final Logger log = LoggerFactory.getLogger(ActionController.class);

    private final RestTemplate restTemplate;
    private final JavaMailSender mailSender;

    public ActionController(RestTemplate restTemplate, ObjectProvider<JavaMailSender> mailProvider) {
        this.restTemplate = restTemplate;
        this.mailSender = mailProvider.getIfAvailable();
    }

    @PostMapping
    public ResponseEntity<?> execute(@RequestBody Map<String, Object> req) {
        String type = String.valueOf(req.getOrDefault("actionType", "http"));
        try {
            if ("email".equalsIgnoreCase(type)) {
                return sendEmail(req);
            }
            return forwardHttp(req);
        } catch (Exception ex) {
            log.error("Execution failed", ex);
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    private ResponseEntity<?> forwardHttp(Map<String, Object> req) {
        String url = (String) req.get("targetUrl");
        Object payload = req.getOrDefault("payload", Map.of());
        @SuppressWarnings("unchecked") Map<String, String> headers = (Map<String, String>) req.getOrDefault("headers", Map.of());
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::add);
        HttpEntity<Object> entity = new HttpEntity<>(payload, httpHeaders);
        log.info("Forwarding HTTP action to {} payload={} headers={}", url, payload, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        return ResponseEntity.status(resp.getStatusCode()).body(Map.of("status", "forwarded", "responseStatus", resp.getStatusCode().value()));
    }

    private ResponseEntity<?> sendEmail(Map<String, Object> req) {
        if (mailSender == null) {
            log.warn("No JavaMailSender configured");
            return ResponseEntity.status(503).body(Map.of("error", "mail-sender-not-configured"));
        }
        String to = (String) req.get("to");
        String subject = (String) req.getOrDefault("subject", "Action Runner Notification");
        String body = String.valueOf(req.getOrDefault("payload", "(no payload)"));
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
        log.info("Sent email to {}", to);
        return ResponseEntity.ok(Map.of("status", "email-sent"));
    }
}
