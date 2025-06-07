package com.streamify.mediahub.dto;

import java.util.Map;

public record ResponseErrorDTO(
        String status,
        String message,
        Map<String, String> moore
) {
}
