#include <CoreServices/CoreServices.h>
#include <jni.h>

#include "util.h"

jobject NewObjectByName(JNIEnv* env, const char* class_name,
                        const char* constructor_sig, ...)
{
    jobject obj = NULL;

    jclass clazz = NULL;
    jmethodID ctor_id;
    va_list args;

    if_unlikely((*env)->EnsureLocalCapacity(env, 2) < 0)
        goto done;

    clazz = (*env)->FindClass(env, class_name);
    if_unlikely(clazz == NULL) {
        goto done;
    }
    ctor_id  = (*env)->GetMethodID(env, clazz, "<init>", constructor_sig);
    if_unlikely(ctor_id == NULL) {
        goto done;
    }
    va_start(args, constructor_sig);
    obj = (*env)->NewObjectV(env, clazz, ctor_id, args);
    va_end(args);

    done:
    (*env)->DeleteLocalRef(env, clazz);
    return obj;
}

jint ThrowByName(JNIEnv* env, const char* name, const char* msg)
{
    jclass clazz = (*env)->FindClass(env, name);
    if_unlikely(clazz == NULL) {
        return (jint)-1;
    }
    return (*env)->ThrowNew(env, clazz, msg);
}

jint ThrowOutOfMemoryError(JNIEnv* env, const char* msg)
{
    return ThrowByName(env, "java/lang/OutOfMemoryError", msg);
}

jint ThrowUnixException(JNIEnv* env, int _errno) {
    jobject t = NewObjectByName(env, "edu/nccu/plsm/watchservice/SunNioFsUnixException", "(I)V", _errno);
    if_unlikely(t == NULL) {
        return -1;
    }
    return (*env)->Throw(env, t);
}

jint _ThrowJNIException(JNIEnv* env, const char* msg, const char* function, const char* file, int line)
{
    jstring method = NULL;
    jstring jfile = NULL;
    jstring message  = NULL;
    jthrowable t;

    method = (*env)->NewStringUTF(env, function);
    if_unlikely(method == NULL) {
        goto no_method;
    }

    jfile = (*env)->NewStringUTF(env, file);
    if_unlikely(jfile == NULL) {
        goto no_file;
    }

    message = (*env)->NewStringUTF(env, msg);
    if_unlikely(message == NULL) {
        goto no_message;
    }

    t = NewObjectByName(
            env,
            "edu/nccu/plsm/watchservice/JNIException",
            "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V",
            method, jfile, line, message
    );
    if_likely(t != NULL) {
        return (*env)->Throw(env, t);
    }

    (*env)->DeleteLocalRef(env, message);
no_message:
    (*env)->DeleteLocalRef(env, jfile);
no_file:
    (*env)->DeleteLocalRef(env, method);
no_method:
    return -1;
}

jint _ThrowJNIExceptionWithCause(JNIEnv* env, const char* msg, jthrowable cause, const char* function, const char* file, int line)
{
    jstring method = NULL;
    jstring jfile = NULL;
    jstring message  = NULL;
    jthrowable t;

    method = (*env)->NewStringUTF(env, function);
    if_unlikely(method == NULL) {
        goto no_method;
    }

    jfile = (*env)->NewStringUTF(env, file);
    if_unlikely(jfile == NULL) {
        goto no_file;
    }

    message = (*env)->NewStringUTF(env, msg);
    if_unlikely(message == NULL) {
        goto no_message;
    }

    t = NewObjectByName(
            env,
            "edu/nccu/plsm/watchservice/JNIException",
            "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/Throwable;)V",
            method, jfile, line, message, cause
    );
    if_likely(t != NULL) {
        return (*env)->Throw(env, t);
    }

    (*env)->DeleteLocalRef(env, message);
no_message:
    (*env)->DeleteLocalRef(env, jfile);
no_file:
    (*env)->DeleteLocalRef(env, method);
no_method:
    (*env)->DeleteLocalRef(env, cause);
    return -1;
}

/**
 * Creates a CF string from the given Java string.
 * If javaString is NULL, NULL is returned.
 * If a memory error occurs, and OutOfMemoryError is thrown and
 * NULL is returned.
 */
CFStringRef toCFString(JNIEnv* env, jstring javaString)
{
    if (javaString == NULL) {
        return NULL;
    } else {
        CFStringRef result = NULL;

        jsize length = (*env)->GetStringLength(env, javaString);
        const jchar* chars = (*env)->GetStringChars(env, javaString, NULL);
        if_unlikely(chars == NULL) {
            ThrowOutOfMemoryError(env, "toCFString: JNIEnv->GetStringChars failure");
            return NULL;
        }
        result = CFStringCreateWithCharacters(NULL, (const UniChar*)chars, length);
        (*env)->ReleaseStringChars(env, javaString, chars);
        if_unlikely(result == NULL) {
            ThrowOutOfMemoryError(env, "toCFString: CFStringCreateWithCharacters failure");
            return NULL;
        }
        return result;
    }
}

/**
 * Creates a Java string from the given CF string.
 * If cfString is NULL, NULL is returned.
 * If a memory error occurs, and OutOfMemoryError is thrown and
 * NULL is returned.
 */
jstring toJavaString(JNIEnv* env, CFStringRef cfString)
{
    if (cfString == NULL) {
        return NULL;
    } else {
        jstring javaString = NULL;

        CFIndex length = CFStringGetLength(cfString);
        const UniChar* constchars = CFStringGetCharactersPtr(cfString);
        if (constchars != NULL) {
            javaString = (*env)->NewString(env, constchars, (jsize)length);
        } else {
            UniChar* chars = malloc(length * sizeof(UniChar));
            if_unlikely(chars == NULL) {
                ThrowOutOfMemoryError(env, "toJavaString: malloc failure");
                return NULL;
            }
            CFStringGetCharacters(cfString, CFRangeMake(0, length), chars);
            javaString = (*env)->NewString(env, chars, (jsize)length);
            free(chars);
        }
        return javaString;
    }
}
