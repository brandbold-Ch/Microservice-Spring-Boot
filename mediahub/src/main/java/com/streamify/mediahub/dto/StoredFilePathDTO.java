package com.streamify.mediahub.dto;
import java.nio.file.Path;

public record StoredFilePathDTO(
        String fileName,
        Path storagePath
) {}
