/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package edu.nccu.plsm.watchservice;

import java.nio.file.*;
import java.io.IOException;

/**
 * Internal exception thrown by native methods when error detected.
 */
// from: sun.nio.fs.UnixException
class SunNioFsUnixException extends Exception {
    static final long serialVersionUID = 7227016794320723218L;

    private final int errno;
    private final String msg;

    SunNioFsUnixException(int errno) {
        this.errno = errno;
        this.msg = null;
    }

    SunNioFsUnixException(String msg) {
        this.errno = 0;
        this.msg = msg;
    }

    int errno() {
        return errno;
    }

    String errorString() {
        if (msg != null) {
            return msg;
        } else {
            return strerror(errno());
        }
    }
    @Override
    public String getMessage() {
        return errorString();
    }

    @Override
    public Throwable fillInStackTrace() {
        // This is an internal exception; the stack trace is irrelevant.
        return this;
    }

    /**
     * Map well known errors to specific exceptions where possible; otherwise
     * return more general FileSystemException.
     */
    private IOException translateToIOException(String file, String other) {
        // created with message rather than errno
        if (msg != null)
            return new IOException(msg);

        // handle specific cases
        if (errno() == EACCES)
            return new AccessDeniedException(file, other, null);
        if (errno() == ENOENT)
            return new NoSuchFileException(file, other, null);
        if (errno() == EEXIST)
            return new FileAlreadyExistsException(file, other, null);
        if (errno() == ELOOP)
            return new FileSystemException(file, other, errorString()
                    + " or unable to access attributes of symbolic link");

        // fallback to the more general exception
        return new FileSystemException(file, other, errorString());
    }

    void rethrowAsIOException(String file) throws IOException {
        throw translateToIOException(file, null);
    }

    void rethrowAsIOException(Path file, Path other) throws IOException {
        String a = (file == null) ? null : file.toString();
        String b = (other == null) ? null : other.toString();
        throw translateToIOException(a, b);
    }

    void rethrowAsIOException(Path file) throws IOException {
        rethrowAsIOException(file, null);
    }

    IOException asIOException(Path file) {
        return translateToIOException(file.toString(), null);
    }

    private static native String strerror(int errno);

    private static final int EACCES = 13;
    private static final int ENOENT = 2;
    private static final int EEXIST = 17;
    private static final int ELOOP = 62;

}
