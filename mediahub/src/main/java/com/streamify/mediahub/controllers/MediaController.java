package com.streamify.mediahub.controllers;

import com.streamify.mediahub.dto.DeleteFilesDto;
import com.streamify.mediahub.dto.ResponseError;
import com.streamify.mediahub.services.MediaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;


@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaService service;

    public MediaController(MediaService service) {
        this.service = service;
    }


    @PostMapping(path = "/upload")
    public ResponseEntity<?> uploadMedia(HttpServletRequest request) {
        Map<String, String> response;
        try {
            response = service.writeMedia(request.getParts());
        } catch (IOException | ServletException | RuntimeException e) {
            ResponseError responseError = new ResponseError(
                    "Error",
                    "An error occurred while uploading content",
                    Map.of("details", e.getMessage()));

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseError);
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/thumbnails/{thumbnailFile}")
    public ResponseEntity<?> getThumbnails(@PathVariable String thumbnailFile) {
        try {
            UrlResource thumbnailData = service.getThumbnail(thumbnailFile);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(thumbnailData);
        } catch (RuntimeException e) {
            ResponseError responseError = new ResponseError(
                    "Error",
                    "An error occurred while displaying the thumbnail",
                    Map.of("details", e.getMessage()));
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseError);
        }
    }

    @GetMapping(value = "/streaming/{fileName}")
    public ResponseEntity<ResourceRegion> contentStream(
            @PathVariable String fileName,
            @RequestHeader HttpHeaders headers) {
        FileSystemResource video = service.getVideoResource(fileName);
        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);

        try {
            long contentLength = video.contentLength();
            ResourceRegion region;

            if (range == null) {
                long chunckSize = Math.min(1_000_000, contentLength);
                region = new ResourceRegion(video, 0, chunckSize);
            } else {
                long start = range.getRangeStart(contentLength);
                long end = range.getRangeEnd(contentLength);
                long rangeLength = Math.min(1_000_000, end - start + 1);
                region = new ResourceRegion(video, start, rangeLength);
            }

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaTypeFactory.getMediaType(video)
                            .orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .body(region);
        } catch (IOException e) {
            return null;
        }
    }

    @ResponseBody
    @DeleteMapping("/")
    public ResponseEntity<?> deleteMedia(@RequestBody DeleteFilesDto request) {
        try {
            service.deleteMedia(
                    request.thumbnailFile(),
                    request.videoFile(),
                    request.trailerFile()
            );
            return ResponseEntity
                    .ok()
                    .build();
        } catch (RuntimeException e) {
            ResponseError responseException = new ResponseError(
                    "Error",
                    "An error occurred while deleting media",
                    Map.of("details", e.getMessage()));
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseException);
        }
    }
}
