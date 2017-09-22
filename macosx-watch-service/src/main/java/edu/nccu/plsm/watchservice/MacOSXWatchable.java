package edu.nccu.plsm.watchservice;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

/**
 * Wrapper for {@link java.nio.file.Path} to bypass the {@link java.nio.file.FileSystem}
 * check by the method {@link java.nio.file.Path#register(WatchService, WatchEvent.Kind[], WatchEvent.Modifier...)}.
 */
public class MacOSXWatchable implements Watchable {
    private static final WatchEvent.Modifier[] EMPTY_MODIFIERS = new WatchEvent.Modifier[0];
    private final Path path;

    public MacOSXWatchable(final Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }


    public Path getPath() {
        return path;
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        if (watcher == null) {
            throw new NullPointerException("watcher");
        } else if (!(watcher instanceof MacOSXWatchService)) {
            throw new ProviderMismatchException();
        } else {
            return ((MacOSXWatchService) watcher).register(path, events, modifiers);
        }
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return this.register(watcher, events, EMPTY_MODIFIERS);
    }


    @Override
    public String toString() {
        return "MacOSXWatchable(path=" + path + ')';
    }
}
