/* Copyright 2015 Intelligent Robotics Group, NASA ARC */

#include <time.h>
#include <string.h>
#include <stdint.h>

#include <jni.h>
#include <android/log.h>

#include <royale.hpp>

#include <forward_list>
#include <utility>
#include <unordered_map>

#include "./tinycthread.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef PICOFLEXX_NEW_API
#  ifdef ROYALE_VERSION_CODE
#    if ROYALE_VERSION_CODE >= ROYALE_VERSION(1, 1, 0, 0)
#      define PICOFLEXX_NEW_API 1
#    else
#      define PICOFLEXX_NEW_API 0
#    endif
#  else
#    define PICOFLEXX_NEW_API 0
#  endif
#endif

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "picoflexx-ndk", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "picoflexx-ndk", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "picoflexx-ndk", __VA_ARGS__))

// Exposure time in microseconds (us).
#define EXPOSURE_TIME 450

#define MAX_POINTS 38304
#define MAX_DATA (MAX_POINTS * 3)
#define MAX_DATA_SIZE (MAX_DATA * sizeof(float))

static JavaVM *gVm;
static std::unique_ptr<royale::ICameraDevice> gCameraDevice;

using time_pair = std::pair<struct timespec, struct timespec>;
static std::forward_list<time_pair> gDataTimes;
static bool gCollectTimes = false;

static jobject gMainActivity;
static jmethodID gOnData;

static void destruct_env(void * data) {
  LOGI("detaching picoflexx thread from VM");
  gVm->DetachCurrentThread();
}

class DataReader : public royale::IDepthDataListener {
 public:
  DataReader()
    : royale::IDepthDataListener(), valid_(false) {
    if (thrd_success != tss_create(&envKey_, destruct_env)) {
      LOGE("unable to create thread local key");
      return;
    }

    valid_ = true;
  }

  void onNewData(const royale::DepthData *data) {
    struct timespec start, end;

    // start profile
    if (gCollectTimes)
      start = getTime();

    // attach to VM
    JNIEnv* env = attachThread();
    if (env == NULL) return;

    // copy data to the bytebuffer
    auto & points = data->points;
    for (size_t i = 0; i < points.size(); ++i) {
      memcpy(&data_[i * 12 + 0], &points[i].x, sizeof(float));
      memcpy(&data_[i * 12 + 4], &points[i].y, sizeof(float));
      memcpy(&data_[i * 12 + 8], &points[i].z, sizeof(float));
    }

    // java callback
    env->CallVoidMethod(gMainActivity, gOnData, buffer_);
    if (env->ExceptionCheck() == JNI_TRUE) {
      LOGE("exception in Java :/");
      return;
    }

    // end profiling
    if (gCollectTimes) {
      end = getTime();
      gDataTimes.push_front(std::make_pair(start, end));
    }
  }

  bool createBuffers(JNIEnv *env) {
    buffer_ = createBuffer(env, data_, sizeof(data_));
    if (buffer_ == NULL) return false;
    return true;
  }

  jobject createBuffer(JNIEnv *env, uint8_t *data, int size) {
    jobject buf = env->NewDirectByteBuffer(reinterpret_cast<void*>(data),
        size);
    if (buf == NULL)
      return NULL;
    return env->NewGlobalRef(buf);
  }

 private:
  bool valid_;
  tss_t envKey_;

  uint8_t data_[MAX_DATA_SIZE];
  jobject buffer_;

  struct timespec getTime() {
    struct timespec now = {0, 0};
    if (clock_gettime(CLOCK_REALTIME, &now) < 0) {
      LOGE("unable to get current time");
    }
    return now;
  }

  JNIEnv * attachThread() {
    JNIEnv *env = reinterpret_cast<JNIEnv*>(tss_get(envKey_));
    if (env == NULL) {
      JavaVMAttachArgs args = { JNI_VERSION_1_6, "picoflexx", NULL };
      if (gVm->AttachCurrentThread(&env, reinterpret_cast<void*>(&args)) != JNI_OK) {
        LOGE("Unable to attach thread");
        return nullptr;
      }
      tss_set(envKey_, reinterpret_cast<void*>(env));
    }

    return env;
  }
};

static DataReader gReader;

#if PICOFLEXX_NEW_API
static const std::unordered_map<int, const char*> kModeMap({
  { 5, "MODE_9_5FPS_2000" },
  { 10, "MODE_9_10FPS_1000" },
  { 15, "MODE_9_15FPS_700" }
});

static int gFps = 5;
#else
static royale::OperationMode gOperationMode = royale::OperationMode::MODE_9_5FPS_2000;
#endif

static jboolean sample_open_picoflexx(JNIEnv* env, jobject  thiz, jint fd) {
  std::unique_ptr<royale::ICameraDevice> cameraDevice;

  unsigned int major, minor, build, patch;
  royale::getVersion(major, minor, patch, build);
  LOGI("libroyale version: %u.%u.%u.%u", major, minor, patch, build);

  // query for any connected cameras.
  // in the case of Android, we pass in an explicit file descriptor, so this should
  // always succeed, assuming the device is still plugged in.
  {
    royale::CameraManager manager;

    auto camlist = manager.getConnectedCameraList(fd);

    if (!camlist.empty()) {
      cameraDevice = manager.createCamera(camlist.at(0));
    }
  }
  // the camera device is now available and CameraManager can be de-allocated here

  if (cameraDevice == nullptr) {
    LOGE("Cannot create the camera device");
    return JNI_FALSE;
  }

  // IMPORTANT: call the initialize method before working with the camera device
  royale::CameraStatus ret = cameraDevice->initialize();
  if (ret != royale::CameraStatus::SUCCESS) {
    LOGE("Cannot initialize the camera device, CODE %d", (int)ret);
    return JNI_FALSE;
  }

  ret = cameraDevice->registerDataListener(&gReader);
  if (ret != royale::CameraStatus::SUCCESS) {
    LOGE("Cannot set the data listener, CODE %d", (int)ret);
    return JNI_FALSE;
  }

#if PICOFLEXX_NEW_API
  auto cases = cameraDevice->getUseCases();
  int idx = -1;
  LOGI("%d use cases", cases.size());
  for (int i = 0; i < cases.size(); i++) {
    if (cases[i] == kModeMap.at(gFps)) {
      idx = i;
      break;
    }

    LOGI(" %s", cases[i].data());
  }

  if (idx == -1) {
    LOGE("Cannot find appropriate mode!");
    return JNI_FALSE;
  }

  // Set the operating mode
  ret = cameraDevice->setUseCase(cases[idx]);
#else
  ret = cameraDevice->setOperationMode(gOperationMode);
#endif
  if (ret != royale::CameraStatus::SUCCESS) {
    LOGE("Cannot set the operating mode, CODE %d", (int)ret);
    return JNI_FALSE;
  }

  // Set manual exposure mode
  ret = cameraDevice->setExposureMode(royale::ExposureMode::MANUAL);
  if (ret != royale::CameraStatus::SUCCESS) {
    LOGE("Cannot set the exposure mode, CODE %d", (int) ret);
    return JNI_FALSE;
  }

  // Set 450us exposure time
  /*
  ret = cameraDevice->setExposureTime(EXPOSURE_TIME);
  if (ret != royale::CameraStatus::SUCCESS) {
    LOGE("Cannot set the exposure time, CODE %d", (int) ret);
    return 1;
  }
  */

  gCameraDevice = std::move(cameraDevice);
  if (!gReader.createBuffers(env)) {
    LOGE("Cannot create bytebuffer");
    return JNI_FALSE;
  }

  gMainActivity = env->NewGlobalRef(thiz);
  if (gMainActivity == NULL) {
    LOGE("Unable to create global reference");
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

static jboolean sample_start(JNIEnv *env, jobject thiz, jboolean profile) {
  if (gCameraDevice == nullptr) {
    LOGW("trying to start capture with a null camera");
    return JNI_FALSE;
  }

  gCollectTimes = (profile == JNI_TRUE);

  LOGI("starting capture...");
  royale::CameraStatus ret = gCameraDevice->startCapture();
  if (ret != royale::CameraStatus::SUCCESS) {
    LOGE("unable to start capture: %d", (int) ret);
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

static jboolean sample_stop(JNIEnv *env, jobject thiz) {
  LOGI("stopping capture...");
  royale::CameraStatus ret = gCameraDevice->stopCapture();
  if (ret != royale::CameraStatus::SUCCESS) {
    LOGE("error stopping capture: %d", (int) ret);
    return JNI_FALSE;
  }

  if (gCollectTimes) {
    gDataTimes.reverse();
    for (auto const &p : gDataTimes) {
      LOGI("callback: start: %.9lf; end: %.9lf",
          p.first.tv_sec + (static_cast<double>(p.first.tv_nsec) / 1e9),
          p.second.tv_sec + (static_cast<double>(p.second.tv_nsec) / 1e9));
    }
    gDataTimes.clear();
  }

  return JNI_TRUE;
}

static void sample_set_exposure(JNIEnv *env, jobject thiz) {
  LOGI("setting exposure mode");
  auto ret = gCameraDevice->setExposureTime(EXPOSURE_TIME);
  if (ret != royale::CameraStatus::SUCCESS) {
    LOGE("Cannot set the exposure time, CODE %d", (int) ret);
    return;
  }
}

static jint sample_get_height(JNIEnv *env, jobject thiz) {
  LOGI("getting max height");
  return gCameraDevice->getMaxSensorHeight();
}

static jint sample_get_width(JNIEnv *env, jobject thiz) {
  LOGI("getting max width");
  return gCameraDevice->getMaxSensorWidth();
}

static JNINativeMethod methods[] = {
  {"openPicoflexx", "(I)Z",  reinterpret_cast<void*>(sample_open_picoflexx) },
  {"startCapturing", "(Z)Z", reinterpret_cast<void*>(sample_start) },
  {"stopCapturing", "()Z",   reinterpret_cast<void*>(sample_stop) },
  {"setExposure", "()V",     reinterpret_cast<void*>(sample_set_exposure) },
  {"getMaxWidth", "()I",     reinterpret_cast<void*>(sample_get_width) },
  {"getMaxHeight", "()I",    reinterpret_cast<void*>(sample_get_height) }
};

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    LOGE("Unable to get JNI Environment");
    return -1;
  }

  jclass clazz = env->FindClass("gov/nasa/arc/irg/astrobee/picoflexx/MainActivity");
  if (clazz == NULL) {
    LOGE("Unable to find activity class");
    return -1;
  }

  // Register methods with env->RegisterNatives.
  if (env->RegisterNatives(clazz, methods, 6) < 0) {
    LOGE("Unable to register native methods");
    return -1;
  }

  // Lookup our callback method
  gOnData = env->GetMethodID(clazz, "onNewData",
      "(Ljava/nio/ByteBuffer;)V");
  if (gOnData == NULL) {
    LOGE("Unable to find onNewData method");
    return -1;
  }

  gVm = vm;

  return JNI_VERSION_1_6;
}

#ifdef __cplusplus
}
#endif
