package pe.org.camaracomercioica.protestos.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class StoragePaths {
    private static final String BACKEND_DIR = "cci-protestos-backend";

    private StoragePaths() {
    }

    public static Path resolveRoot(String location) {
        Path configured = Paths.get(location == null || location.isBlank() ? "storage" : location);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }

        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (BACKEND_DIR.equals(userDir.getFileName().toString())) {
            return userDir.resolve(configured).normalize();
        }

        Path backendDir = userDir.resolve(BACKEND_DIR);
        if (backendDir.toFile().isDirectory()) {
            return backendDir.resolve(configured).normalize();
        }

        return userDir.resolve(configured).normalize();
    }
}
