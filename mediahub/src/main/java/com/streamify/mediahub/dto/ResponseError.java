package com.streamify.mediahub.dto;

import java.util.Map;

public record ResponseError(
        String status,
        String message,
        Map<String, String> moore
) {
}
