#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_pfs_android_daemon_Daemon_nativeSupportLibraries (JNIEnv * env, jclass /*clazz*/)
{
    char const * libs [] = {"lib1", "lib2", "lib3" };
    int count = sizeof(libs) / sizeof(libs[0]);
    jobjectArray result = env->NewObjectArray(count, env->FindClass("java/lang/String"), 0);

    for (int i = 0; i < count; i++) {
        auto str = env->NewStringUTF(libs[i]);
        env->SetObjectArrayElement(result, i, str);
    }

    return result;
}
