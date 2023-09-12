package ui.Helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

public class Passwords {
    public static String readPassword(String key) {
        try {
            final Path path = Paths.get(System.getProperty("user.home"), ".viollier", "passwords");
            String osName = System.getProperty("os.name");
            if(!osName.contains("Windows")){
                final Set<PosixFilePermission> posixFilePermissions = Files.getPosixFilePermissions(path);
                if (posixFilePermissions.contains(PosixFilePermission.GROUP_READ) || posixFilePermissions.contains(PosixFilePermission.OTHERS_READ)) {
                    throw new IllegalStateException("Passwords are too exposed. Do 'chmod 600 ~/.viollier/passwords'");
                }
            }
            return Files.readAllLines(path).stream()
                    .map(l -> {
                        final String[] pieces = l.split("=", 2);
                        return Map.entry(pieces[0], pieces[1]);
                    })
                    .filter(p -> p.getKey().equals(key))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
