package com.example.security.service.impl;

import com.example.security.config.FileStorageProperties;
import com.example.security.exception.BadRequestException;
import com.example.security.exception.UserNotFoundException;
import com.example.security.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final List<String> ALLOWED_EXT = List.of(".jpg", ".jpeg", ".png", ".pdf");

    private final FileStorageProperties props;

    @Override
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("File tidak boleh kosong");

        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "file";
        String ext = getExtension(originalName).toLowerCase();
        if (!ALLOWED_EXT.contains(ext))
            throw new BadRequestException("Ekstensi file tidak diizinkan. Gunakan: " + ALLOWED_EXT);

        try {
            Path dir = Paths.get(props.getBaseDir(), subDir);
            Files.createDirectories(dir);

            String fileName = UUID.randomUUID() + "_" + originalName.replaceAll("\\s+", "_");
            Path target = dir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Kembalikan path relatif yang disimpan di DB
            return subDir + "/" + fileName;
        } catch (IOException e) {
            log.error("Gagal menyimpan file: {}", e.getMessage());
            throw new RuntimeException("Gagal menyimpan file: " + e.getMessage());
        }
    }

    @Override
    public Resource load(String relativePath) {
        try {
            Path file = Paths.get(props.getBaseDir()).resolve(relativePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            throw new UserNotFoundException("File tidak ditemukan: " + relativePath);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gagal membaca file {}: {}", relativePath, e.getMessage());
            throw new RuntimeException("Gagal membaca file: " + relativePath);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path file = Paths.get(props.getBaseDir()).resolve(relativePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Gagal menghapus file {}: {}", relativePath, e.getMessage());
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
