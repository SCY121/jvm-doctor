package io.github.jvmdoctor.server;

import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class ArtifactStorage {

    private final Path rootDirectory;

    public ArtifactStorage() {
        try {
            this.rootDirectory = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "jvm-doctor"));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize artifact storage", exception);
        }
    }

    public Artifact storeUpload(MultipartFile file, ArtifactType type) {
        try {
            String originalName = file.getOriginalFilename() == null ? type.name().toLowerCase() : file.getOriginalFilename();
            Path target = rootDirectory.resolve(UUID.randomUUID() + "-" + originalName);
            file.transferTo(target);
            return new Artifact(type, originalName, target);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store uploaded file", exception);
        }
    }

    public Artifact storeText(String source, ArtifactType type, String suffix, String content) {
        try {
            Path target = Files.createTempFile(rootDirectory, type.name().toLowerCase() + "-", suffix);
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return new Artifact(type, source, target);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store generated artifact", exception);
        }
    }
}
