package edu.nccu.plsm.watchservice;

import com.sun.nio.file.ExtendedWatchEventModifier;
import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.stream.Stream;

final class MacOSXWatchServiceImpl extends MacOSXWatchService {
    private final Poller poller;
    MacOSXWatchServiceImpl() {
        this.poller = new Poller(this);
        this.poller.start();
        this.poller.awaitInit();
    }

    @Override
    final WatchKey register(Path dir,
                            WatchEvent.Kind<?>[] events,
                            WatchEvent.Modifier... modifiers)
            throws IOException {
        return poller.register(dir, events, modifiers);
    }

    @Override
    final void implClose() throws IOException {
        poller.close();
    }

    /**
     * WatchKey implementation
     */
    private static class MacOSXWatchKey extends SunNioFsAbstractWatchKey {
        private Set<? extends WatchEvent.Kind<?>> events;
        private boolean watchSubtree;

        private final Path realPath;
        private final Object fileKey;
        private final Set<Path> snapshot;

        private long fsEventStream;
        private volatile boolean valid;

        MacOSXWatchKey(Path dir, MacOSXWatchServiceImpl watcher,
                       boolean watchSubtree, Set<? extends WatchEvent.Kind<?>> events,
                       long fsEventStream, Object fileKey, Set<Path> snapshot) throws IOException {
            super(dir, watcher);
            this.realPath = dir.toRealPath();
            this.fsEventStream = fsEventStream;
            this.fileKey = fileKey;
            this.events = events;
            this.snapshot = snapshot;
            this.valid = true;
            this.watchSubtree = watchSubtree;
        }

        /**
         * Config which event to watch by this key.
         * @param events events
         */
        void setEvents(final Set<? extends WatchEvent.Kind<?>> events) {
            this.events = events;
        }

        /**
         * Return the event watched by this key.
         * @return events
         */
        Set<? extends WatchEvent.Kind<?>> getEvents() {
            return events;
        }

        void setWatchSubtree(boolean watchSubtree) {
            this.watchSubtree = watchSubtree;
        }

        boolean isWatchSubtree() {
            return watchSubtree;
        }

        public Path getRealPath() {
            return realPath;
        }

        public Object getFileKey() {
            return fileKey;
        }

        public Set<Path> getSnapshot() {
            return snapshot;
        }

        public long getFSEventStream() {
            return fsEventStream;
        }

        void invalidate() {
            snapshot.clear();
            valid = false;
        }

        @Override
        final public boolean isValid() {
            return valid;
        }

        @Override
        final public void cancel() {
            if (isValid()) {
                ((MacOSXWatchServiceImpl)watcher()).poller.cancel(this);
            }
        }
    }

    /**
     * Background thread for CFRunLoop
     */
    private static class Poller extends SunNioFsAbstractPoller {
        private final MacOSXWatchServiceImpl watcher;
        private final Map<Long, MacOSXWatchKey> fsEventStreamToKey;
        private final Map<Object, MacOSXWatchKey> fileKeyToKey;
        private final Object initLock = new Object();
        // pointer
        private long cfRunLoop = 0;
        private long signalSource = 0;

        private List<MacOSXWatchKey> pendingClose = new LinkedList<>();
        private long lastReceivedEventId = 0;

        Poller(MacOSXWatchServiceImpl watcher) {
            this.watcher = watcher;
            this.fsEventStreamToKey = new HashMap<>();
            this.fileKeyToKey = new HashMap<>();
        }

        @Override
        void wakeup() throws IOException {
            cfRunLoopSignal(cfRunLoop, signalSource);
        }

        @Override
        Object implRegister(final Path dir,
                            final Set<? extends WatchEvent.Kind<?>> events,
                            final WatchEvent.Modifier... modifiers) {

            LatencyWatchEventModifier latency = LatencyWatchEventModifier.DEFAULT;
            boolean noDefer = false;
            boolean watchSubtree = false;
            if (modifiers.length > 0) {
                // use last modifier as the same behavior as sun.nio.fs.LinuxWatchService
                for (WatchEvent.Modifier modifier: modifiers) {
                    if (modifier == null) {
                        return new NullPointerException();
                    }
                    if (modifier == ExtendedWatchEventModifier.FILE_TREE) {
                        watchSubtree = true;
                    } else if (modifier == NoDeferWatchEventModifier.INSTANCE) {
                        noDefer = true;
                    } else if (modifier instanceof LatencyWatchEventModifier) {
                        latency = (LatencyWatchEventModifier)modifier;
                    } else if (modifier instanceof SensitivityWatchEventModifier) {
                        latency = new LatencyWatchEventModifier((SensitivityWatchEventModifier)modifier);
                    } else {
                        return new UnsupportedOperationException("Modifier not supported");
                    }
                }
            }

            final double latencyInSeconds = latency.getLatencyInSeconds();
            final boolean finalWatchSubtree = watchSubtree;
            final boolean finalNoDefer = noDefer;
            try {
                return AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    // check file is directory
                    PosixFileAttributes attrs = Files.readAttributes(dir, PosixFileAttributes.class);
                    if (!attrs.isDirectory()) {
                        return new NotDirectoryException(dir.toString());
                    }
                    Object fileKey = attrs.fileKey();
                    if (fileKey == null) {
                        return new AssertionError("File keys must be support");
                    }

                    MacOSXWatchKey watchKey = fileKeyToKey.get(fileKey);
                    if (watchKey == null) {
                        Set<Path> snapshot = new HashSet<>();
                        populateSnapshot(dir, snapshot, true);
                        long flag = CREATE_FLAG;
                        if (finalNoDefer) {
                            flag |= FS_EVENT_STREAM_CREATE_FLAG_NO_DEFER;
                        }
                        long fsEventStream = createFSEventStream(cfRunLoop, dir.toString(),latencyInSeconds, CREATE_FLAG);
                        watchKey = new MacOSXWatchKey(dir, watcher, finalWatchSubtree, events, fsEventStream, fileKey, snapshot);
                        fileKeyToKey.put(fileKey, watchKey);
                        fsEventStreamToKey.put(fsEventStream, watchKey);
                    } else {
                        if (watchKey.isWatchSubtree() != finalWatchSubtree) {
                            watchKey.setWatchSubtree(finalWatchSubtree);
                            watchKey.getSnapshot().clear();
                            populateSnapshot(dir, watchKey.getSnapshot(), true);
                        }

                        watchKey.setEvents(events);
                    }
                    return watchKey;
                });
            } catch (PrivilegedActionException e) {
                return e.getException();
            }
        }

        // cancel single key
        @Override
        void implCancelKey(WatchKey obj) {
            MacOSXWatchKey key = (MacOSXWatchKey)obj;
            if (key.isValid()) {
                fsEventStreamToKey.remove(key.getFSEventStream());
                fileKeyToKey.remove(key.getFileKey());
                closeKey(key);
            }
        }

        // close watch service
        @Override
        void implCloseAll() {
            // invalidate all keys
            fsEventStreamToKey.values().forEach(this::closeKey);
            fsEventStreamToKey.clear();
            fileKeyToKey.clear();
            cfRunLoopStop(cfRunLoop);
        }

        @Override
        public void run() {
            synchronized (initLock) {
                cfRunLoop = cfRunLoopGetCurrent();
                signalSource = createSignalSource();
                initLock.notifyAll();
            }
            cfRunLoopRunWithSignalSource(signalSource);
        }

        void awaitInit() {
            boolean interrupted = false;
            synchronized (initLock) {
                while (cfRunLoop == 0) {
                    try {
                        initLock.wait();
                    } catch (InterruptedException x) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void closeKey(MacOSXWatchKey key) {
            key.invalidate();
            stopFSEventsStream(cfRunLoop, key.getFSEventStream());
        }

        private static void populateSnapshot(Path path, Set<Path> snapshot, boolean subTree) throws IOException {
            if (subTree) {
                try (Stream<Path> stream = Files.walk(path)) {
                    // skip self
                    stream.skip(1).map(path::relativize).forEach(snapshot::add);
                }
            } else {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    stream.forEach(p -> snapshot.add(p.getFileName()));
                } catch (DirectoryIteratorException e) {
                    throw e.getCause();
                }
            }
        }

        /*
         * only one type of modify, create, remove, rename
         */
        private static boolean containsOnlyOneEventEntry(final int eventFlag) {
            int flag = eventFlag & WATCH_MASK;
            return ((flag & ~ENTRY_MODIFIED_MASK) == 0)
                    || flag == FS_EVENT_STREAM_EVENT_FLAG_ITEM_REMOVED
                    || flag == FS_EVENT_STREAM_EVENT_FLAG_ITEM_CREATED
                    || flag == FS_EVENT_STREAM_EVENT_FLAG_ITEM_RENAMED;
        }

        private void onWakeup() {
            Iterator<MacOSXWatchKey> it = pendingClose.iterator();
            while (it.hasNext()) {
                MacOSXWatchKey key = it.next();
                stopFSEventsStream(cfRunLoop, key.getFSEventStream());
                it.remove();
            }
            processRequests();
        }



        private static void signalIfWatched(MacOSXWatchKey key, WatchEvent.Kind<?> event, Path path) {
            if (key.getEvents().contains(event)) {
                key.signalEvent(event, path);
            }
        }

        private void onFsEvent(final long streamAddress, final String eventPath, final int eventFlag, final long eventId) {
            if ((eventFlag | FS_EVENT_STREAM_EVENT_FLAG_IDS_WRAPPED) > 0) {
                lastReceivedEventId = 0;
            }
            lastReceivedEventId = Math.max(eventId, lastReceivedEventId);

            MacOSXWatchKey key = fsEventStreamToKey.get(streamAddress);
            if (key != null) {
                if ((eventFlag & FS_EVENT_STREAM_EVENT_FLAG_MUST_SCAN_SUB_DIRS) > 0) {
                    Path path = Paths.get(eventPath);
                    Set<Path> snapshot = key.getSnapshot();
                    snapshot.clear();
                    try {
                        populateSnapshot(path, snapshot, key.isWatchSubtree());
                    } catch (IOException ignored) {
                    }
                    key.signalEvent(StandardWatchEventKinds.OVERFLOW, null);
                    return;
                }

                if ((eventFlag & FS_EVENT_STREAM_EVENT_FLAG_ROOT_CHANGED) > 0) {
                    fsEventStreamToKey.remove(streamAddress);
                    key.invalidate();
                    // workaround:
                    // close stream with "closeKey(key);" in callback will cause error
                    // (FSEvents.framework) process_dir_events: watch_path: error trying to add kqueue for fd
                    // (FSEvents.framework) process_dir_events: watch_path: error removing fd 16 from kqueue
                    //
                    //  delaying next wakeup
                    pendingClose.add(key);
                    key.signal();
                    return;
                }

                // if is event interested
                if ((eventFlag & WATCH_MASK) > 0) {
                    Path path = Paths.get(eventPath);
                    Path relativePath = key.getRealPath().relativize(path);

                    // if not in subdir
                    if (key.isWatchSubtree() || relativePath.getNameCount() == 1) {
                        Set<Path> snapshot = key.getSnapshot();

                        // fast path to signal filesystem event if the event flag is safe
                        if (containsOnlyOneEventEntry(eventFlag)) {
                            if ((eventFlag & FS_EVENT_STREAM_EVENT_FLAG_ITEM_RENAMED) > 0) {
                                if (snapshot.remove(relativePath)) {
                                    signalIfWatched(key, StandardWatchEventKinds.ENTRY_DELETE, relativePath);
                                } else {
                                    snapshot.add(relativePath);
                                    signalIfWatched(key, StandardWatchEventKinds.ENTRY_CREATE, relativePath);
                                }
                            } else if ((eventFlag & FS_EVENT_STREAM_EVENT_FLAG_ITEM_CREATED) > 0) {
                                snapshot.add(relativePath);
                                signalIfWatched(key, StandardWatchEventKinds.ENTRY_CREATE, relativePath);
                            } else if ((eventFlag & FS_EVENT_STREAM_EVENT_FLAG_ITEM_REMOVED) > 0) {
                                snapshot.remove(relativePath);
                                signalIfWatched(key, StandardWatchEventKinds.ENTRY_DELETE, relativePath);
                            } else if ((eventFlag & ENTRY_MODIFIED_MASK) > 0) {
                                // update the snapshot in case we miss some events
                                snapshot.add(relativePath);
                                signalIfWatched(key, StandardWatchEventKinds.ENTRY_MODIFY, relativePath);
                            }
                        } else {
                            // can't not trust the event flag because it contains multiple type of events
                            // bug or intended behavior?
                            // https://github.com/haskell-fswatch/hfsnotify/issues/36
                            // https://stackoverflow.com/questions/18415285/osx-fseventstreameventflags-not-working-correctly
                            // fallback to check

                            boolean existInSnapshot = snapshot.contains(relativePath);
                            boolean existInFileSystem = Files.exists(path);
                            if (existInFileSystem) {
                                if (existInSnapshot) {
                                    if ((eventFlag & ENTRY_MODIFIED_MASK) > 0) {
                                        key.signalEvent(StandardWatchEventKinds.ENTRY_MODIFY, relativePath);
                                    }
                                } else {
                                    snapshot.add(relativePath);
                                    if ((eventFlag & ENTRY_CREATED_MASK) > 0) {
                                        signalIfWatched(key, StandardWatchEventKinds.ENTRY_CREATE, relativePath);
                                    }
                                    // we may miss create event
                                    if ((eventFlag & ENTRY_MODIFIED_MASK) > 0) {
                                        signalIfWatched(key, StandardWatchEventKinds.ENTRY_MODIFY, relativePath);
                                    }
                                }
                            } else if (existInSnapshot) {
                                snapshot.remove(relativePath);
                                if ((eventFlag & ENTRY_DELETED_MASK) > 0) {
                                    signalIfWatched(key, StandardWatchEventKinds.ENTRY_DELETE, relativePath);
                                }
                            }
                        }
                    }
                }

            }
        }

        // -- native methods --
        static {
            JNILoader.load("mws-jni");
        }

        private native long createFSEventStream(long cfRunLoop, String path, double latency, long flags);
        private native void stopFSEventsStream(long cfRunLoop, long fsEventStream);
        private native long createSignalSource();

        private static native void cfRunLoopSignal(long cfRunLoop, long signalSource);
        private static native long cfRunLoopGetCurrent();
        private static native void cfRunLoopRunWithSignalSource(long signalSource);
        private static native void cfRunLoopStop(long cfRunLoop);

        // event id
        private static native long kFSEventStreamEventIdSinceNow();

        // create flag
        private static native int kFSEventStreamCreateFlagNone();
        private static native int kFSEventStreamCreateFlagUseCFTypes();
        private static native int kFSEventStreamCreateFlagNoDefer();
        private static native int kFSEventStreamCreateFlagWatchRoot();
        private static native int kFSEventStreamCreateFlagIgnoreSelf();
        private static native int kFSEventStreamCreateFlagFileEvents();
        private static native int kFSEventStreamCreateFlagMarkSelf();

        // event flag
        private static native int kFSEventStreamEventFlagNone();
        private static native int kFSEventStreamEventFlagMustScanSubDirs();
        private static native int kFSEventStreamEventFlagUserDropped();
        private static native int kFSEventStreamEventFlagKernelDropped();
        private static native int kFSEventStreamEventFlagEventIdsWrapped();
        private static native int kFSEventStreamEventFlagHistoryDone();
        private static native int kFSEventStreamEventFlagRootChanged();
        private static native int kFSEventStreamEventFlagMount();
        private static native int kFSEventStreamEventFlagUnmount();
        private static native int kFSEventStreamEventFlagItemCreated();
        private static native int kFSEventStreamEventFlagItemRemoved();
        private static native int kFSEventStreamEventFlagItemInodeMetaMod();
        private static native int kFSEventStreamEventFlagItemRenamed();
        private static native int kFSEventStreamEventFlagItemModified();
        private static native int kFSEventStreamEventFlagItemFinderInfoMod();
        private static native int kFSEventStreamEventFlagItemChangeOwner();
        private static native int kFSEventStreamEventFlagItemXattrMod();
        private static native int kFSEventStreamEventFlagItemIsFile();
        private static native int kFSEventStreamEventFlagItemIsDir();
        private static native int kFSEventStreamEventFlagItemIsSymlink();
        private static native int kFSEventStreamEventFlagOwnEvent();
        private static native int kFSEventStreamEventFlagItemIsHardlink();
        private static native int kFSEventStreamEventFlagItemIsLastHardlink();

        // event id
        private static final long FS_EVENT_STREAM_EVENT_ID_SINCE_NOW = kFSEventStreamEventIdSinceNow();
        // create flag
        private static final long FS_EVENT_STREAM_CREATE_FLAG_NONE = kFSEventStreamCreateFlagNone();
        private static final long FS_EVENT_STREAM_CREATE_FLAG_USE_CF_TYPES = kFSEventStreamCreateFlagUseCFTypes();
        private static final long FS_EVENT_STREAM_CREATE_FLAG_NO_DEFER = kFSEventStreamCreateFlagNoDefer();
        private static final long FS_EVENT_STREAM_CREATE_FLAG_WATCH_ROOT = kFSEventStreamCreateFlagWatchRoot();
        private static final long FS_EVENT_STREAM_CREATE_FLAG_IGNORE_SELF = kFSEventStreamCreateFlagIgnoreSelf();
        private static final long FS_EVENT_STREAM_CREATE_FLAG_FILE_EVENTS = kFSEventStreamCreateFlagFileEvents();
        private static final long FS_EVENT_STREAM_CREATE_FLAG_MARK_SELF = kFSEventStreamCreateFlagMarkSelf();
        // event flag
        private static final int FS_EVENT_STREAM_EVENT_FLAG_NONE = kFSEventStreamEventFlagNone();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_MUST_SCAN_SUB_DIRS = kFSEventStreamEventFlagMustScanSubDirs();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_USER_DROPPED = kFSEventStreamEventFlagUserDropped();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_KERNEL_DROPPED = kFSEventStreamEventFlagKernelDropped();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_IDS_WRAPPED = kFSEventStreamEventFlagEventIdsWrapped();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_HISTORY_DONE = kFSEventStreamEventFlagHistoryDone();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ROOT_CHANGED = kFSEventStreamEventFlagRootChanged();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_MOUNT = kFSEventStreamEventFlagMount();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_UNMOUNT = kFSEventStreamEventFlagUnmount();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_CREATED = kFSEventStreamEventFlagItemCreated();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_REMOVED = kFSEventStreamEventFlagItemRemoved();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_INODE_META_MODIFIED = kFSEventStreamEventFlagItemInodeMetaMod();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_RENAMED = kFSEventStreamEventFlagItemRenamed();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_MODIFIED = kFSEventStreamEventFlagItemModified();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_FINDER_INFO_MODIFIED = kFSEventStreamEventFlagItemFinderInfoMod();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_CHANGE_OWNER = kFSEventStreamEventFlagItemChangeOwner();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_XATTR_MODIFIED = kFSEventStreamEventFlagItemXattrMod();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_IS_FILE = kFSEventStreamEventFlagItemIsFile();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_IS_DIR = kFSEventStreamEventFlagItemIsDir();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_IS_SYMLINK = kFSEventStreamEventFlagItemIsSymlink();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_OWN_EVENT = kFSEventStreamEventFlagOwnEvent();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_IS_HARD_LINK = kFSEventStreamEventFlagItemIsHardlink();
        private static final int FS_EVENT_STREAM_EVENT_FLAG_ITEM_IS_LAST_HARD_LINK = kFSEventStreamEventFlagItemIsLastHardlink();

        private static final long CREATE_FLAG = FS_EVENT_STREAM_CREATE_FLAG_NONE
                | FS_EVENT_STREAM_CREATE_FLAG_FILE_EVENTS
                | FS_EVENT_STREAM_CREATE_FLAG_WATCH_ROOT;

        private static final int ENTRY_CREATED_MASK = FS_EVENT_STREAM_EVENT_FLAG_ITEM_CREATED
                | FS_EVENT_STREAM_EVENT_FLAG_ITEM_RENAMED;

        private static final int ENTRY_DELETED_MASK = FS_EVENT_STREAM_EVENT_FLAG_ITEM_REMOVED
                | FS_EVENT_STREAM_EVENT_FLAG_ITEM_RENAMED;

        private static final int ENTRY_MODIFIED_MASK = FS_EVENT_STREAM_EVENT_FLAG_ITEM_MODIFIED
                | FS_EVENT_STREAM_EVENT_FLAG_ITEM_INODE_META_MODIFIED
                | FS_EVENT_STREAM_EVENT_FLAG_ITEM_FINDER_INFO_MODIFIED
                | FS_EVENT_STREAM_EVENT_FLAG_ITEM_CHANGE_OWNER
                | FS_EVENT_STREAM_EVENT_FLAG_ITEM_XATTR_MODIFIED;

        private static final int WATCH_MASK = ENTRY_MODIFIED_MASK | ENTRY_CREATED_MASK | ENTRY_DELETED_MASK;
    }
}
