#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <sys/wait.h>
#include <dirent.h>
#include <signal.h>

static int throw_runtime_exception(JNIEnv *env, char const *message)
{
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, exClass, message);
    return -1;
}

int createSession(JNIEnv *env, int *processID, char const *cmd, char **cmdArgs, char const *cwd)
{
    int ptm = open("/dev/ptmx", O_RDWR | O_NOCTTY);
    char ptsName[64];

    if (ptm < 0)
        return -100; // throw_runtime_exception(env, "Cannot create PTM");
    if (grantpt(ptm) == -1 || unlockpt(ptm) == -1 || ptsname_r(ptm, ptsName, strlen(ptsName)) == -1)
        return -101; // throw_runtime_exception(env, "Cannot grantpt / unlockpt / ptsname_r");

    struct termios tios;
    tcgetattr(ptm, &tios);
    tios.c_iflag |= IUTF8;
    tios.c_iflag &= ~(IXON | IXOFF);
    tcsetattr(ptm, TCSANOW, &tios);

    struct winsize sz = {.ws_row = 1000, .ws_col = 1000};
    ioctl(ptm, TIOCSWINSZ, &sz);

    int childPID = fork();
    if (childPID < 0)
        return -102; // throw_runtime_exception(env, "Cannot create child process");
    if (childPID > 0){
        *processID = childPID;
        return ptm;
    }

    sigset_t signals_to_unblock;
    sigfillset(&signals_to_unblock);
    sigprocmask(SIG_UNBLOCK, &signals_to_unblock, 0);

    close(ptm);
    setsid();

    int pts = open(ptsName, O_RDWR);

    dup2(pts, STDIN_FILENO);
    dup2(pts, STDOUT_FILENO);
    dup2(pts, STDERR_FILENO);

    DIR *self_dir = opendir("/proc/self/fd");
    if (self_dir != NULL)
    {
        int self_dir_fd = dirfd(self_dir);
        struct dirent *entry;
        while ((entry = readdir(self_dir)) != NULL)
        {
            int fd = atoi(entry->d_name);
            if (fd > 2 && fd != self_dir_fd)
                close(fd);
        }
        closedir(self_dir);
    }

    if (chdir(cwd) != 0)
        return -103; // throw_runtime_exception(env, "Cannot chdir ");

    execvp(cmd, cmdArgs);
    return -104; // throw_runtime_exception(env, "Cannot execvp() command");
}

JNIEXPORT jint JNICALL Java_com_prashantrawatcoder_bashterminal_TerminalSession_createTerminalSession(JNIEnv *env,
                                                                                                      jobject thiz,
                                                                                                      jstring cmd,
                                                                                                      jstring cwd,
                                                                                                      jobjectArray args,
                                                                                                      jintArray processIdArray)
{
    jsize size = args ? (*env)->GetArrayLength(env, args) : 0;
    char **argv = NULL;
    if (size > 0)
    {
        argv = (char **)malloc((size + 1) * sizeof(char *));
        if (!argv)
            return throw_runtime_exception(env, "Couldn't allocate argv array");

        for (int i = 0; i < size; ++i)
        {
            jstring arg_java_string = (jstring)(*env)->GetObjectArrayElement(env, args, i);
            char const *arg_utf8 = (*env)->GetStringUTFChars(env, arg_java_string, NULL);
            if (!arg_utf8)
                return throw_runtime_exception(env, "GetStringUTFChars() failed for argv");
            argv[i] = strdup(arg_utf8);
            (*env)->ReleaseStringUTFChars(env, arg_java_string, arg_utf8);
        }
        argv[size] = NULL;
    }

    int processID = 0;
    char const *cmd_cwd = (*env)->GetStringUTFChars(env, cwd, NULL);
    char const *cmd_utf8 = (*env)->GetStringUTFChars(env, cmd, NULL);
    int ptm = createSession(env, &processID, cmd_utf8, argv, cmd_cwd);
    (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);
    (*env)->ReleaseStringUTFChars(env, cmd, cmd_cwd);

    if (argv)
    {
        for (char **tmp = argv; *tmp; ++tmp)
            free(*tmp);
        free(argv);
    }

    int *pProcId = (int *)(*env)->GetPrimitiveArrayCritical(env, processIdArray, NULL);
    if (!pProcId)
        return throw_runtime_exception(env, "JNI call GetPrimitiveArrayCritical(processIdArray, &isCopy) failed");

    *pProcId = processID;
    (*env)->ReleasePrimitiveArrayCritical(env, processIdArray, pProcId, 0);

    return ptm;
}