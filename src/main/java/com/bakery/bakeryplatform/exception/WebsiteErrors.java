package com.bakery.bakeryplatform.exception;
import com.bakery.bakeryplatform.service.user.SiteIdentity;
import com.bakery.bakeryplatform.controller.website.SiteCartController;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice(basePackageClasses = SiteCartController.class)
public class WebsiteErrors {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> http(ResponseStatusException error) {
        String message = error.getReason() == null ? "Request failed." : error.getReason();
        return ResponseEntity.status(error.getStatusCode())
                .body(SiteIdentity.map("success", false, "message", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> duplicate() {
        return ResponseEntity.status(409).body(SiteIdentity.map("success", false, "message",
                "This record conflicts with existing data. Please check it and try again."));
    }
}
