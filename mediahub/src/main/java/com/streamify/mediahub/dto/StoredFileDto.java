package com.streamify.mediahub.dto;
import java.nio.file.Path;
import java.util.Map;

public record StoredFileDto(
        String fileName,
        Path storagePath
) {}
