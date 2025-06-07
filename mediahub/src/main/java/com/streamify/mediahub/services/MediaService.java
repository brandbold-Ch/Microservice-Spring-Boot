package com.streamify.mediahub.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;
import com.streamify.mediahub.dto.StoredFilePathDTO;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Part;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class MediaService {

    private final Map<String, String> contentTypesImages;
    private final Map<String, String> contentTypesVideos;
    private Path thumbnailsDir;
    private Path moviesDir;
    private Path trailersDir;

    @Value("${media.base-dir}")
    private String baseDir;

    @PostConstruct
    public void init() {
        Path basePath = Paths.get(baseDir);

        thumbnailsDir = initAndGetDir(basePath, "thumbnails");
        moviesDir = initAndGetDir(basePath, "movies");
        trailersDir = initAndGetDir(basePath, "trailers");
    }

    private Path initAndGetDir(Path basePath, String subDir) {
        Path fullPath = basePath.resolve(subDir);
        try {
            Files.createDirectories(fullPath);
        } catch (IOException e) {
            throw new RuntimeException("Error creating directory: " + fullPath, e);
        }
        return fullPath;
    }

    public MediaService() {
        contentTypesImages = new HashMap<>();
        contentTypesVideos = new HashMap<>();

        contentTypesImages.put("image/jpeg", "jpg");
        contentTypesImages.put("image/png", "png");
        contentTypesImages.put("image/gif", "gif");
        contentTypesImages.put("image/bmp", "bmp");
        contentTypesImages.put("image/webp", "webp");
        contentTypesImages.put("image/svg+xml", "svg");

        contentTypesVideos.put("video/mp4", "mp4");
        contentTypesVideos.put("video/x-msvideo", "avi");
        contentTypesVideos.put("video/quicktime", "mov");
        contentTypesVideos.put("video/x-ms-wmv", "wmv");
        contentTypesVideos.put("video/x-matroska", "mkv");
        contentTypesVideos.put("video/webm", "webm");
        contentTypesVideos.put("video/x-flv", "flv");
    }

    private Path getFilePath(String file, String dir) {
        switch (dir) {
            case "thumbnails" -> {
                return thumbnailsDir.resolve(file);
            }
            case "movies" -> {
                return moviesDir.resolve(file);
            }
            case "trailers" -> {
                return trailersDir.resolve(file);
            }
            default -> {
                throw new RuntimeException("Directory not found.");
            }
        }
    }

    private boolean inContentTypesVideos(String format) {
        return contentTypesVideos.containsKey(format);
    }

    private boolean inContentTypesImages(String format) {
        return contentTypesImages.containsKey(format);
    }

    private StoredFilePathDTO createNewFilePath(String customName, String tag, String format) {
        String extension;
        String outputDir;

        switch (tag) {
            case "trailerFile", "contentFile", "chapterFile" -> {
                if (!inContentTypesVideos(format)) {
                    throw new RuntimeException("Unsupported video format: " + format);
                }
                extension = contentTypesVideos.get(format);
                outputDir = tag.equals("trailerFile") ? "trailers" : "movies";
            }
            case "thumbnailFile" -> {
                if (!inContentTypesImages(format)) {
                    throw new RuntimeException("Unsupported image format: " + format);
                }
                extension = contentTypesImages.get(format);
                outputDir = "thumbnails";
            }
            default -> throw new RuntimeException("Unsupported tag: " + tag);
        }
        String fileName = "%s.%s".formatted(customName, extension);
        Path storagePath = getFilePath(fileName, outputDir);

        return new StoredFilePathDTO(fileName, storagePath);
    }

    public Map<String, String> writeMedia(Collection<Part> parts) {
        Map<String, String> names = new HashMap<>();
        String customName = String.valueOf(UUID.randomUUID());

        for (Part part : parts) {
            try {
                StoredFilePathDTO storedFile = createNewFilePath(customName, part.getName(),
                        part.getContentType());
                Files.copy(part.getInputStream(), storedFile.storagePath(),
                        StandardCopyOption.REPLACE_EXISTING);
                names.put(part.getName(), storedFile.fileName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return names;
    }

    public UrlResource getThumbnail(String thumbnailFile) {
        Path file = thumbnailsDir.resolve(thumbnailFile);
        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteMedia(String thumbnailFile, String contentFile, String trailerFile) {
        try {
            if (thumbnailFile != null) {
                Files.deleteIfExists(thumbnailsDir.resolve(thumbnailFile));
            }
            if (contentFile != null) {
                Files.deleteIfExists(moviesDir.resolve(contentFile));
            }
            if (trailerFile != null) {
                Files.deleteIfExists(trailersDir.resolve(trailerFile));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileSystemResource getResource(String fileName, String source) {
        switch (source) {
            case "movie" -> {
                return getMovieResource(fileName);
            }
            case "trailer" -> {
                return getTrailerResource(fileName);
            }
            default -> throw new RuntimeException("The source does not exist");
        }
    }

    public FileSystemResource getMovieResource(String fileName) {
        return new FileSystemResource(moviesDir.resolve(fileName));
    }

    public FileSystemResource getTrailerResource(String fileName) {
        return new FileSystemResource(trailersDir.resolve(fileName));
    }
}
