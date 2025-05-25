package com.streamify.mediahub.dto;

public record DeleteFilesDto(
        String thumbnailFile,
        String videoFile,
        String trailerFile
) {}
