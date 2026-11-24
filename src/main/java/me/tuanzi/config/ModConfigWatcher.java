package me.tuanzi.config;

import me.tuanzi.util.ModLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 配置文件后台热加载监听器：基于 WatchService 监听 config.json 变更并自动重新加载。
 */
public final class ModConfigWatcher implements AutoCloseable {
    private static final long SELF_WRITE_IGNORE_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    private static final long DEBOUNCE_MILLIS = 120L;

    private final Path configPath;
    private final Consumer<Path> onReload;
    private volatile boolean running = true;
    private WatchService watchService;
    private Thread watcherThread;

    public ModConfigWatcher(Path configPath, Consumer<Path> onReload) {
        this.configPath = configPath.toAbsolutePath().normalize();
        this.onReload = Objects.requireNonNull(onReload, "onReload");
    }

    public synchronized void start() {
        if (this.watcherThread != null) {
            return;
        }
        try {
            Path dir = this.configPath.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            Path watchDir = dir != null ? dir : Path.of(".");
            this.watchService = FileSystems.getDefault().newWatchService();
            watchDir.register(
                    this.watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );
            this.watcherThread = new Thread(this::watchLoop, "TuanzisMod-ConfigWatcher");
            this.watcherThread.setDaemon(true);
            this.watcherThread.start();
            ModLog.debug("[ModConfigWatcher] 已启动配置文件后台热加载监听: " + this.configPath);
        } catch (IOException e) {
            ModLog.error("[ModConfigWatcher] 启动配置文件监听失败: " + e.getMessage(), e);
            close();
        }
    }

    private void watchLoop() {
        String targetFileName = this.configPath.getFileName().toString();
        while (this.running) {
            WatchKey key;
            try {
                key = this.watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            boolean matched = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                Object context = event.context();
                if (context instanceof Path changed && changed.getFileName().toString().equals(targetFileName)) {
                    matched = true;
                }
            }
            boolean valid = key.reset();
            if (matched && this.running) {
                handlePossibleFileChange();
            }
            if (!valid) {
                break;
            }
        }
    }

    private void handlePossibleFileChange() {
        try {
            // 等待外部编辑器完成写入（如先清空再写入）
            Thread.sleep(DEBOUNCE_MILLIS);
            if (!this.running || !Files.exists(this.configPath)) {
                return;
            }

            // 清空防抖期间积累的重复事件
            WatchKey pending;
            while ((pending = this.watchService.poll()) != null) {
                pending.pollEvents();
                pending.reset();
            }

            if (System.nanoTime() - ModConfig.getLastSelfWriteNanos() < SELF_WRITE_IGNORE_NANOS) {
                String currentContent = readNonEmptyContent();
                if (currentContent != null && currentContent.equals(ModConfig.getLastKnownJsonContent())) {
                    return;
                }
            }

            String currentContent = readNonEmptyContent();
            if (currentContent == null || currentContent.equals(ModConfig.getLastKnownJsonContent())) {
                return;
            }

            ModLog.debug("[ModConfigWatcher] 检测到配置文件被外部修改，触发自动热加载: " + this.configPath);
            this.onReload.accept(this.configPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException ignored) {
            // 关闭时忽略
        } catch (Exception e) {
            ModLog.error("[ModConfigWatcher] 处理配置文件热加载时出错: " + e.getMessage(), e);
        }
    }

    private String readNonEmptyContent() throws IOException, InterruptedException {
        String raw = Files.readString(this.configPath, StandardCharsets.UTF_8);
        if (raw.isBlank()) {
            Thread.sleep(100L);
            raw = Files.readString(this.configPath, StandardCharsets.UTF_8);
        }
        return raw.isBlank() ? null : raw;
    }

    @Override
    public synchronized void close() {
        this.running = false;
        if (this.watchService != null) {
            try {
                this.watchService.close();
            } catch (IOException ignored) {
            }
            this.watchService = null;
        }
        if (this.watcherThread != null) {
            this.watcherThread.interrupt();
            this.watcherThread = null;
        }
    }
}
