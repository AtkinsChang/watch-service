#ifndef MACOSX_WATCH_SERVICE_JNI_H
#define MACOSX_WATCH_SERVICE_JNI_H

#include <CoreFoundation/CoreFoundation.h>
#include <CoreServices/CoreServices.h>
#include <jni.h>

#define FSEventStreamCleanup(stream) \
{ \
    FSEventStreamInvalidate(stream); \
    FSEventStreamRelease(stream); \
}

#define CFRunLoopSourceCleanup(source) \
{ \
    CFRunLoopSourceInvalidate(source); \
    CFRelease(source); \
}

typedef struct {
    JNIEnv* env;
    jobject obj;
    jmethodID methodId;
} JNIMethodInvocationInfo;
typedef JNIMethodInvocationInfo SignalCallbackContext;
typedef JNIMethodInvocationInfo FSEventCallbackContext;

static void release_jni_method_invocation_info(const void* arg);
static void on_wakeup(void *arg);
static void on_fs_event(
        ConstFSEventStreamRef streamRef,
        void *clientCallBackInfo,
        size_t numEvents,
        void *eventPaths,
        const FSEventStreamEventFlags *eventFlags,
        const FSEventStreamEventId *eventIds
);

#endif //MACOSX_WATCH_SERVICE_JNI_H
