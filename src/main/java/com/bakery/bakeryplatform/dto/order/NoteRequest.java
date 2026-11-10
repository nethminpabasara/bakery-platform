package com.bakery.bakeryplatform.dto.order;

import jakarta.validation.constraints.Size;

public record NoteRequest(
        @Size(max = 200, message = "Note must be 200 characters or less")
        String note) {
}