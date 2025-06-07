package com.streamify.mediahub.dto;

public record DeleteFilesDTO(
        String thumbnailFile,
        String contentFile,
        String trailerFile
) {}
