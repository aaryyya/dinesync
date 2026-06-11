package com.dinesync.dinesync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponse {
    private String sessionToken;
    private Integer tableId;
    private String message;

    public CheckInResponse(String sessionToken, Integer tableId) {
        this.sessionToken = sessionToken;
        this.tableId = tableId;
        this.message = "Check-in successful! Welcome to Table " + tableId;
    }
}
