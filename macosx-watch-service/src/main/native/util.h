#ifndef MACOSX_WATCH_SERVICE_UTIL_H
#define MACOSX_WATCH_SERVICE_UTIL_H

#include <CoreServices/CoreServices.h>
#include <jni.h>
#include <string.h>

#ifndef jlong_to_ptr
#define jlong_to_ptr(a) ((void*)(a))
#endif

#ifndef ptr_to_jlong
#define ptr_to_jlong(a) ((jlong)(a))
#endif

#ifndef likely
#define likely(x)      __builtin_expect(!!(x), 1)
#endif

#ifndef unlikely
#define unlikely(x)    __builtin_expect(!!(x), 0)
#endif

#define if_unlikely(x)    if(unlikely(x))
#define if_likely(x)    if(likely(x))

#define __FILENAME__ (strrchr(__FILE__, '/') ? strrchr(__FILE__, '/') + 1 : __FILE__)

#define ThrowJNIException(env, msg) \
    _ThrowJNIException(env, msg, __func__, __FILENAME__, __LINE__)
#define ThrowJNIExceptionWithCause(env, msg, cause) \
    _ThrowJNIExceptionWithCause(env, msg, cause, __func__, __FILENAME__, __LINE__)

/* Construct a new object of class, specifying the class by name,
 * and specififying which constructor to run and what arguments to
 * pass to it.
 *
 * The method will return an initialized instance if successful.
 * It will return NULL if an error has occurred (for example if
 * it ran out of memory) and the appropriate Java exception will
 * have been thrown.
 */
jobject NewObjectByName(JNIEnv* env, const char* class_name,
                        const char* constructor_sig, ...);

jint ThrowByName(JNIEnv* env, const char* name, const char* msg);
jint ThrowOutOfMemoryError(JNIEnv* env, const char* msg);
jint ThrowUnixException(JNIEnv* env, int _errno);
jint _ThrowJNIException(JNIEnv* env, const char* msg, const char* function, const char* file, int line);
jint _ThrowJNIExceptionWithCause(JNIEnv* env, const char* msg, jthrowable cause, const char* function, const char* file, int line);

CFStringRef toCFString(JNIEnv* env, jstring javaString);
jstring toJavaString(JNIEnv* env, CFStringRef cfString);

#endif //MACOSX_WATCH_SERVICE_UTIL_H
