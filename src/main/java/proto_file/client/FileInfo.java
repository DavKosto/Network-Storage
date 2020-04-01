package proto_file.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FileInfo {
    private boolean directory;
    private Path path;
    private String filename;
    private long size;
    private LocalDateTime lastModified;

    boolean isDirectory() {
        return directory;
    }

    String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    FileInfo(Path path) {
        try {
            this.path = path;
            this.filename = path.getFileName().toString();
            this.size = Files.size(path);
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
        } catch (IOException e) {
            throw new RuntimeException("Unable to create file info from path");
        }
    }

    public FileInfo(Path path, boolean isDirectory, long size, LocalDateTime lastModified) {
        this.path = path;
        this.directory = isDirectory;
        this.filename = path.getFileName().toString();
        this.size = size;
        this.lastModified = lastModified;
    }
}
