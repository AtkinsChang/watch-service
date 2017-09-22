package edu.nccu.plsm.watchservice;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

class JNILoader {
    private static final Path DEFAULT_LIBRARY_BASE = Paths.get("META-INF", "native");
    private static final Path DEFAULT_TEMP_DIRECTORY;
    static {
        String tmpDir = System.getProperty("edu.nccu.plsm.watchservice.tmpdir");
        Path tmpPath;
        if (tmpDir != null) {
            try {
                tmpPath = Paths.get(tmpDir).toRealPath();
            } catch (IOException e) {
                tmpPath = Paths.get(System.getProperty("java.io.tmpdir"));
            }
        } else {
            tmpPath = Paths.get(System.getProperty("java.io.tmpdir"));
        }
        DEFAULT_TEMP_DIRECTORY = tmpPath;
    }

    public static void load(String name) {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                try {
                    System.loadLibrary(name);
                    return null;
                } catch(Throwable ignored) {
                }
                String libname = System.mapLibraryName(name);
                Path libpath = DEFAULT_LIBRARY_BASE.resolve(libname);
                URL libUrl = JNILoader.class.getClassLoader().getResource(libpath.toString());
                if (libUrl == null) {
                    throw new FileNotFoundException(libpath.toString());
                }
                Path tmpDir = Files.createTempDirectory(DEFAULT_TEMP_DIRECTORY, name);
                tmpDir.toFile().deleteOnExit();
                Path tmpLibPath = tmpDir.resolve(libname);
                try {
                    Files.copy(libUrl.openStream(), tmpLibPath);
                    System.load(tmpLibPath.toString());
                } finally {
                    Files.deleteIfExists(tmpLibPath);
                }
                return null;
            });
        } catch (PrivilegedActionException pe) {
            Error e = new UnsatisfiedLinkError("could not load a jni library: " + name);
            e.initCause(pe.getException());
            throw e;
        }
    }
}
