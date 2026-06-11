package com.dinesync.dinesync.controller;

import com.dinesync.dinesync.model.CheckInResponse;
import com.dinesync.dinesync.service.SessionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@Validated  // enables constraint annotations on path variables (C3 fix)
public class SessionController {

    @Autowired
    private SessionService sessionService;

    /**
     * Called when a customer scans the QR code.
     * Table IDs must be 1–999 to prevent garbage session flooding.
     */
    @PostMapping("/checkin/{tableId}")
    public ResponseEntity<CheckInResponse> checkIn(
            @PathVariable @Min(1) @Max(999) Integer tableId) {
        String token = sessionService.createSession(tableId);
        return ResponseEntity.ok(new CheckInResponse(token, tableId));
    }
}
