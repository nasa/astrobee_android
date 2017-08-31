
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libusb
LOCAL_SRC_FILES := royale/lib/libusb_android.so
include $(PREBUILT_SHARED_LIBRARY)

ifneq ("$(wildcard $(LOCAL_PATH)/royale/lib/libuvc.so)", "")
include $(CLEAR_VARS)
LOCAL_MODULE := libuvc
LOCAL_SRC_FILES := royale/lib/libuvc.so
include $(PREBUILT_SHARED_LIBRARY)
endif

include $(CLEAR_VARS)
LOCAL_MODULE := libroyale
LOCAL_SRC_FILES := royale/lib/libroyale.so
LOCAL_EXPORT_C_INCLUDES := royale/include
LOCAL_SHARED_LIBRARIES := libusb
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE           := picoflexx
LOCAL_SRC_FILES := \
    sample.cpp \
    tinycthread.c

LOCAL_CFLAGS := -DTARGET_PLATFORM_ANDROID
LOCAL_LDLIBS :=  -llog -ldl
LOCAL_SHARED_LIBRARIES += libroyale
include $(BUILD_SHARED_LIBRARY)
