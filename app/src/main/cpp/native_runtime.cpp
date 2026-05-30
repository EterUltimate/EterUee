#include <jni.h>

#include <arpa/inet.h>
#include <dirent.h>
#include <errno.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>

#include <string>

namespace {

int deleteTree(const char* path) {
    struct stat info {};
    if (lstat(path, &info) != 0) {
        return errno == ENOENT ? 0 : errno;
    }

    if (S_ISDIR(info.st_mode) && !S_ISLNK(info.st_mode)) {
        DIR* dir = opendir(path);
        if (dir == nullptr) return errno;

        int firstError = 0;
        while (dirent* entry = readdir(dir)) {
            const char* name = entry->d_name;
            if (name[0] == '.' && (name[1] == '\0' || (name[1] == '.' && name[2] == '\0'))) {
                continue;
            }

            std::string child(path);
            child.push_back('/');
            child.append(name);

            const int childResult = deleteTree(child.c_str());
            if (childResult != 0 && firstError == 0) {
                firstError = childResult;
            }
        }
        closedir(dir);

        if (rmdir(path) != 0 && firstError == 0) {
            firstError = errno;
        }
        return firstError;
    }

    if (unlink(path) != 0) {
        return errno == ENOENT ? 0 : errno;
    }
    return 0;
}

bool isTcpPortAvailable(const int port) {
    if (port < 1 || port > 65535) return false;

    const int fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (fd < 0) return false;

    sockaddr_in addr {};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_ANY);
    addr.sin_port = htons(static_cast<uint16_t>(port));

    const bool available = bind(fd, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) == 0;
    close(fd);
    return available;
}

}  // namespace

extern "C" JNIEXPORT jint JNICALL
Java_com_eterultimate_eteruee_runtime_NativeRuntime_nativeDeleteTree(
    JNIEnv* env,
    jobject /* thiz */,
    jstring path
) {
    if (path == nullptr) return EINVAL;

    const char* chars = env->GetStringUTFChars(path, nullptr);
    if (chars == nullptr) return ENOMEM;

    const int result = deleteTree(chars);
    env->ReleaseStringUTFChars(path, chars);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eterultimate_eteruee_runtime_NativeRuntime_nativeIsTcpPortAvailable(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint port
) {
    return isTcpPortAvailable(static_cast<int>(port)) ? JNI_TRUE : JNI_FALSE;
}
