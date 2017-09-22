#include <CoreFoundation/CoreFoundation.h>
#include <CoreServices/CoreServices.h>
#include <jni.h>

#include "edu_nccu_plsm_watchservice_SunNioFsUnixException.h"
#include "edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_Poller.h"
#include "util.h"
#include "macosx_watch_service.h"


static jmethodID pollerOnWakeUp = NULL;
static jmethodID pollerOnFsEvent = NULL;

JNIEXPORT jstring JNICALL Java_edu_nccu_plsm_watchservice_SunNioFsUnixException_strerror
        (JNIEnv* env, jclass clazz, jint _errno) {
    char buf[1024];
    if_unlikely(_errno == 0) {
        buf[0] = 0;
    } else {
        strerror_r(_errno, buf, sizeof(buf));
    }
    return (*env)->NewStringUTF(env, buf);
}

JNIEXPORT jlong JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventIdSinceNow
        (JNIEnv* env, jclass clazz) {
    return (jlong)kFSEventStreamEventIdSinceNow;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamCreateFlagNone
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamCreateFlagNone;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamCreateFlagUseCFTypes
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamCreateFlagUseCFTypes;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamCreateFlagNoDefer
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamCreateFlagNoDefer;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamCreateFlagWatchRoot
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamCreateFlagWatchRoot;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamCreateFlagIgnoreSelf
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamCreateFlagIgnoreSelf;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamCreateFlagFileEvents
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamCreateFlagFileEvents;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamCreateFlagMarkSelf
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamCreateFlagMarkSelf;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagNone
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagNone;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagMustScanSubDirs
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagMustScanSubDirs;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagUserDropped
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagUserDropped;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagKernelDropped
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagKernelDropped;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagEventIdsWrapped
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagEventIdsWrapped;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagHistoryDone
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagHistoryDone;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagRootChanged
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagRootChanged;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagMount
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagMount;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagUnmount
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagUnmount;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemCreated
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemCreated;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemRemoved
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemRemoved;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemInodeMetaMod
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemInodeMetaMod;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemRenamed
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemRenamed;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemModified
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemModified;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemFinderInfoMod
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemFinderInfoMod;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemChangeOwner
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemChangeOwner;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemXattrMod
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemXattrMod;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemIsFile
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemIsFile;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemIsDir
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemIsDir;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemIsSymlink
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemIsSymlink;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagOwnEvent
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagOwnEvent;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemIsHardlink
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemIsHardlink;
}

JNIEXPORT jint JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_kFSEventStreamEventFlagItemIsLastHardlink
        (JNIEnv* env, jclass clazz) {
    return (jint)kFSEventStreamEventFlagItemIsLastHardlink;
}

JNIEXPORT jlong JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_createFSEventStream
        (JNIEnv* env, jobject obj, jlong runLoop, jstring jpath, jdouble latency, jlong flags) {
    FSEventStreamRef ref = NULL;

    jobject g_obj = NULL;
    CFStringRef path = NULL;
    CFArrayRef paths = NULL;
    FSEventCallbackContext* f_ctx = NULL;
    FSEventStreamContext ctx;

    // path
    path = toCFString(env, jpath);
    if_unlikely(path == NULL) {
        goto no_path;
    }

    // path[]
    paths = CFArrayCreate(kCFAllocatorDefault, (const void**)&path, 1, &kCFTypeArrayCallBacks);
    if_unlikely(paths == NULL) {
        ThrowOutOfMemoryError(env, "CFArrayCreate failure");
        goto no_paths;
    }

    // g_obj
    g_obj = (*env)->NewGlobalRef(env, obj);
    if_unlikely(g_obj == NULL) {
        ThrowOutOfMemoryError(env, "JNIEnv->NewGlobalRef");
        goto no_g_obj;
    }

    // f_ctx
    f_ctx = malloc(sizeof(FSEventCallbackContext));
    if_unlikely(f_ctx == NULL) {
        ThrowOutOfMemoryError(env, "malloc failure");
        goto no_f_ctx;
    }
    f_ctx->env = env;
    f_ctx->obj = g_obj;
    f_ctx->methodId = pollerOnFsEvent;

    ctx = (FSEventStreamContext){ 0, f_ctx, NULL, release_jni_method_invocation_info, NULL };
    ref = FSEventStreamCreate(
            kCFAllocatorDefault,
            &on_fs_event,
            &ctx,
            paths,
            kFSEventStreamEventIdSinceNow,
            (CFTimeInterval) latency,
            (FSEventStreamCreateFlags) flags
    );
    if_unlikely(ref == NULL) {
        // todo: will errno be set?
        ThrowJNIException(env, "FSEventStreamCreate");
        goto no_stream;
    }

    FSEventStreamScheduleWithRunLoop(
            ref,
            (CFRunLoopRef)jlong_to_ptr(runLoop),
            kCFRunLoopDefaultMode
    );

    if_unlikely(!FSEventStreamStart(ref)) {
        // todo: will errno be set?
        ThrowJNIException(env, "FSEventStreamCreate");
        goto no_start;
    }

    // success
    goto success;

    // cleanup
no_start:
    FSEventStreamUnscheduleFromRunLoop(
            ref,
            (CFRunLoopRef)jlong_to_ptr(runLoop),
            kCFRunLoopDefaultMode
    );
    FSEventStreamCleanup(ref);
    ref = NULL;
no_stream:
    free(f_ctx);
no_f_ctx:
    (*env)->DeleteGlobalRef(env, g_obj);
no_g_obj:
success:
    CFRelease(paths);
no_paths:
    CFRelease(path);
no_path:
    return ptr_to_jlong(ref);
}

JNIEXPORT void JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_stopFSEventsStream
        (JNIEnv* env, jclass clazz, jlong runLoop, jlong jstream) {
    FSEventStreamRef stream = jlong_to_ptr(jstream);
    FSEventStreamStop(stream);
    FSEventStreamUnscheduleFromRunLoop(
            stream,
            (CFRunLoopRef)jlong_to_ptr(runLoop),
            kCFRunLoopDefaultMode
    );
    FSEventStreamCleanup(stream);
}

JNIEXPORT jlong JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_createSignalSource
        (JNIEnv* env, jobject obj) {
    CFRunLoopSourceRef ref = NULL;

    jobject g_obj = NULL;
    SignalCallbackContext* s_ctx = NULL;
    CFRunLoopSourceContext ctx;

    // g_obj
    g_obj = (*env)->NewGlobalRef(env, obj);
    if_unlikely(g_obj == NULL) {
        ThrowOutOfMemoryError(env, "JNIEnv->NewGlobalRef");
        goto no_g_obj;
    }

    // s_ctx
    s_ctx = malloc(sizeof(SignalCallbackContext));
    if_unlikely(s_ctx == NULL) {
        ThrowOutOfMemoryError(env, "malloc failure");
        goto no_s_ctx;
    }
    s_ctx->env = env;
    s_ctx->obj = (*env)->NewGlobalRef(env, obj);
    s_ctx->methodId = pollerOnWakeUp;

    ctx = (CFRunLoopSourceContext){0, s_ctx, NULL, release_jni_method_invocation_info, NULL, NULL, NULL, NULL, NULL, on_wakeup };
    ref = CFRunLoopSourceCreate(kCFAllocatorDefault, (CFIndex)0, &ctx);
    if_unlikely(ref == NULL) {
        // todo: will errno be set?
        ThrowJNIException(env, "CFRunLoopSourceCreate");
        goto no_source;
    }
    // success
    goto success;

    //cleanup
no_source:
    free(s_ctx);
no_s_ctx:
    (*env)->DeleteGlobalRef(env, g_obj);
no_g_obj:
success:
    return ptr_to_jlong(ref);
}

JNIEXPORT void JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_cfRunLoopSignal
        (JNIEnv* env, jclass clazz, jlong runLoop, jlong source) {
    CFRunLoopSourceSignal((CFRunLoopSourceRef)jlong_to_ptr(source));
    CFRunLoopWakeUp((CFRunLoopRef)jlong_to_ptr(runLoop));
}


JNIEXPORT jlong JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_cfRunLoopGetCurrent
        (JNIEnv* env, jclass clazz) {
    return ptr_to_jlong(CFRunLoopGetCurrent());
}


JNIEXPORT void JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_cfRunLoopRunWithSignalSource
        (JNIEnv* env, jclass clazz, jlong jsource) {
    CFRunLoopSourceRef source = jlong_to_ptr(jsource);
    CFRunLoopAddSource(
            CFRunLoopGetCurrent(),
            source,
            kCFRunLoopDefaultMode
    );
    CFRunLoopRun();
    CFRunLoopSourceCleanup(source);
}

JNIEXPORT void JNICALL Java_edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_00024Poller_cfRunLoopStop
        (JNIEnv* env, jclass clazz, jlong runLoop) {
    CFRunLoopStop((CFRunLoopRef)jlong_to_ptr(runLoop));
}

static void release_jni_method_invocation_info(const void* arg) {
    const JNIMethodInvocationInfo* ctx = arg;
    (*ctx->env)->DeleteGlobalRef(ctx->env, ctx->obj);
    free(ctx);
}

static void on_wakeup(void *arg) {
    SignalCallbackContext* ctx = arg;
    JNIEnv* env = ctx->env;
    if_unlikely((*env)->ExceptionCheck) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    (*env)->CallBooleanMethod(env, ctx->obj, ctx->methodId);
}

static void on_fs_event(
        ConstFSEventStreamRef streamRef,
        void *clientCallBackInfo,
        size_t numEvents,
        void *eventPaths,
        const FSEventStreamEventFlags *eventFlags,
        const FSEventStreamEventId *eventIds) {
    FSEventCallbackContext* ctx = clientCallBackInfo;
    JNIEnv* env = ctx->env;
    char** paths = (char**)eventPaths;
    for (size_t i = 0; i < numEvents; i++) {
        if_unlikely((*env)->ExceptionCheck) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        (*env)->CallVoidMethod(
                env,
                ctx->obj,
                ctx->methodId,
                ptr_to_jlong(streamRef),
                (*env)->NewStringUTF(env, paths[i]),
                (jint)eventFlags[i],
                (jlong)eventIds[i]
        );
    }
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    jint version = JNI_VERSION_1_8;
    JNIEnv* env;
    jint getEnvResult = (*vm)->GetEnv(vm, (void**) &env, version);
    if (getEnvResult != JNI_OK) {
        version = getEnvResult;
        goto no_env;
    }

    jclass l_macOSXWatchServicePollerClass = (*env)->FindClass(env, "edu/nccu/plsm/watchservice/MacOSXWatchServiceImpl$Poller");
    if_unlikely(l_macOSXWatchServicePollerClass == NULL) {
        if_unlikely((*env)->ExceptionCheck == JNI_FALSE) {
            // will this happen?
            ThrowJNIException(env, "JNIEnv->GetObjectClass");
        }
        version = JNI_ERR;
        goto no_class;
    }

    pollerOnWakeUp = (*env)->GetMethodID(env, l_macOSXWatchServicePollerClass, "onWakeup", "()V");
    if_unlikely(pollerOnWakeUp == NULL) {
        if_unlikely((*env)->ExceptionCheck == JNI_FALSE) {
            // THIS SHOULD NOT HAPPEN
            ThrowJNIException(env, "JNIEnv->GetMethodID");
        }
        version = JNI_ERR;
        goto no_method;
    }

    pollerOnFsEvent = (*env)->GetMethodID(env, l_macOSXWatchServicePollerClass, "onFsEvent", "(JLjava/lang/String;IJ)V");
    if_unlikely(pollerOnFsEvent == NULL) {
        if_unlikely((*env)->ExceptionCheck == JNI_FALSE) {
            // THIS SHOULD NOT HAPPEN
            ThrowJNIException(env, "JNIEnv->GetMethodID");
        }
        version = JNI_ERR;
        goto no_method;
    }

    // success
    return version;

    // cleanup
no_method:
    (*env)->DeleteLocalRef(env, l_macOSXWatchServicePollerClass);
no_class:
no_env:
    return version;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) {

}