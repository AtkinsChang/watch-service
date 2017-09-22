package edu.nccu.plsm.watchservice;

final class JNIException extends RuntimeException {
    static final long serialVersionUID = 8742689_2L;

    JNIException(String method, String file, int line, String message) {
        super(message);
        initJNIDetail(method, file, line);
    }


    JNIException(String method, String file, int line, String message, Throwable cause) {
        super(message, cause);
        initJNIDetail(method, file, line);
    }

    private void initJNIDetail(String method, String file, int line) {
        StackTraceElement[] stackTraces = getStackTrace();
        StackTraceElement[] stackTracesWithNativeInfo = new StackTraceElement[stackTraces.length + 1];
        stackTracesWithNativeInfo[0] = new StackTraceElement("<Native>", method, file, line);
        System.arraycopy(stackTraces, 0, stackTracesWithNativeInfo, 1, stackTraces.length);
        setStackTrace(stackTracesWithNativeInfo);
    }

}
