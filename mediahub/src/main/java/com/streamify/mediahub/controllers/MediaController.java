package com.streamify.mediahub.controllers;

import com.streamify.mediahub.dto.DeleteFilesDTO;
import com.streamify.mediahub.dto.ResponseErrorDTO;
import com.streamify.mediahub.services.MediaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.Map;


@RestController
@RequestMapping("/media")
public class MediaController {

    @Value("${flask.server}")
    private String flaskUrl;

    private final MediaService service;
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    public MediaController(MediaService service) {
        this.service = service;
    }

    @PostMapping(path = "/upload")
    public ResponseEntity<?> uploadMedia(HttpServletRequest request) {
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String[]> metadata = request.getParameterMap();
        HttpEntity<Map<String, Object>> entity;
        Map<String, Object> response;

        try{
            response = service.writeMedia(request.getParts());

            for (String key : metadata.keySet()) {
                String[] value = metadata.get(key);

                if (service.hasUUID(value) || value.length == 0) {
                    response.put(key, value);
                } else {
                    response.put(key, value[0]);
                }
            }
            entity = new HttpEntity<>(response, headers);

        } catch (IOException | ServletException | RuntimeException e) {
            ResponseErrorDTO responseError = new ResponseErrorDTO(
                    "Error",
                    "An error occurred while uploading content",
                    Map.of("details", e.getMessage()));

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseError);
        }
        return restTemplate.postForEntity(flaskUrl, entity, String.class);
    }

    @GetMapping("/thumbnails/{thumbnailFile}")
    public ResponseEntity<?> getThumbnails(@PathVariable String thumbnailFile) {
        try {
            UrlResource thumbnailData = service.getThumbnail(thumbnailFile);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(thumbnailData);
        } catch (RuntimeException e) {
            ResponseErrorDTO responseError = new ResponseErrorDTO(
                    "Error",
                    "An error occurred while displaying the thumbnail",
                    Map.of("details", e.getMessage()));
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseError);
        }
    }

    @GetMapping(value = "/streaming/{fileName}/")
    public ResponseEntity<ResourceRegion> contentStream(
            @PathVariable String fileName,
            @RequestParam String source,
            @RequestHeader HttpHeaders headers) {
        FileSystemResource video = service.getResource(fileName, source);
        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);

        try {
            long contentLength = video.contentLength();
            ResourceRegion region;

            if (range == null) {
                long chunkSize = Math.min(1_000_000, contentLength);
                region = new ResourceRegion(video, 0, chunkSize);
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
    public ResponseEntity<?> deleteMedia(@RequestBody DeleteFilesDTO request) {
        try {
            service.deleteMedia(
                    request.thumbnailFile(),
                    request.contentFile(),
                    request.trailerFile()
            );
            return ResponseEntity
                    .ok()
                    .build();
        } catch (RuntimeException e) {
            ResponseErrorDTO responseException = new ResponseErrorDTO(
                    "Error",
                    "An error occurred while deleting media",
                    Map.of("details", e.getMessage()));
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseException);
        }
    }
}
