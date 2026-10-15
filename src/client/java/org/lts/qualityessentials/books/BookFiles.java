package org.lts.qualityessentials.books;

import com.mojang.blaze3d.Blaze3D;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.lts.qualityessentials.ModLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class BookFiles {
    public static final long MAX_BYTES = 512 * 1024;
    private static final ModLog LOG = ModLog.of("Books");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private BookFiles() {
    }

    public record Entry(Path path, String name, long size, long modified) {
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("qualityessentials").resolve("books");
    }

    public static String fileName(String base, LocalDateTime time) {
        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < base.length() && clean.length() < 40; i++) {
            char c = base.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                clean.append(c);
            } else if ((c == ' ' || c == '.') && !clean.isEmpty() && clean.charAt(clean.length() - 1) != '_') {
                clean.append('_');
            }
        }
        while (!clean.isEmpty() && clean.charAt(clean.length() - 1) == '_') {
            clean.setLength(clean.length() - 1);
        }
        String name = clean.isEmpty() ? "book" : clean.toString();
        return name + "_" + STAMP.format(time) + ".txt";
    }

    public static void save(String base, String content, Consumer<Path> done, Consumer<Throwable> failed) {
        LocalDateTime now = LocalDateTime.now();
        CompletableFuture.supplyAsync(() -> {
            try {
                Path dir = dir();
                Files.createDirectories(dir);
                Path file = dir.resolve(fileName(base, now));
                for (int i = 2; Files.exists(file); i++) {
                    file = dir.resolve(fileName(base, now).replace(".txt", "_" + i + ".txt"));
                }
                Path temp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                Files.move(temp, file);
                return file;
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }, Util.ioPool()).whenComplete((file, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                LOG.warn("Could not save the book: {}", ModLog.describe(error));
                failed.accept(error);
            } else {
                LOG.info("Saved the book to {}", file.getFileName());
                done.accept(file);
            }
        }));
    }

    public static void list(Consumer<List<Entry>> done) {
        CompletableFuture.supplyAsync(() -> {
            List<Entry> entries = new ArrayList<>();
            Path dir = dir();
            if (!Files.isDirectory(dir)) {
                return entries;
            }
            try (Stream<Path> files = Files.list(dir)) {
                for (Path path : files.toList()) {
                    String name = path.getFileName().toString();
                    if (Files.isRegularFile(path) && (name.endsWith(".txt") || name.endsWith(".md"))) {
                        entries.add(new Entry(path, name, Files.size(path), Files.getLastModifiedTime(path).toMillis()));
                    }
                }
            } catch (IOException exception) {
                LOG.warn("Could not list {}: {}", dir, ModLog.describe(exception));
            }
            entries.sort(Comparator.comparingLong(Entry::modified).reversed());
            return entries;
        }, Util.ioPool()).thenAccept(entries -> Minecraft.getInstance().execute(() -> done.accept(entries)));
    }

    public static void read(Path path, Consumer<String> done, Consumer<Throwable> failed) {
        CompletableFuture.supplyAsync(() -> {
            try {
                if (Files.size(path) > MAX_BYTES) {
                    throw new IllegalStateException("too large");
                }
                return Files.readString(path, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }, Util.ioPool()).whenComplete((text, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                LOG.warn("Could not read {}: {}", path.getFileName(), ModLog.describe(error));
                failed.accept(error);
            } else {
                done.accept(text);
            }
        }));
    }

    public static void openFolder() {
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(dir());
                Blaze3D.openPath(dir());
            } catch (Exception exception) {
                LOG.warn("Could not open {}: {}", dir(), ModLog.describe(exception));
            }
        }, Util.ioPool());
    }
}
