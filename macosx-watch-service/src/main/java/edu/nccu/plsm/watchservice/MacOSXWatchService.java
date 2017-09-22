package edu.nccu.plsm.watchservice;

import java.nio.file.Path;
import java.nio.file.WatchService;
import java.nio.file.Watchable;

public abstract class MacOSXWatchService extends SunNioFsAbstractWatchService {
    MacOSXWatchService() {
        super();
    }

    public static WatchService newInstance() {
        return new MacOSXWatchServiceImpl();
    }

    public static Watchable wrap(Path path) {
        return new MacOSXWatchable(path);
    }
}