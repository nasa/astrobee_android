NDK_TOOLCHAIN_VERSION=4.9
APP_ABI := armeabi #armeabi-v7a x86
APP_PLATFORM := android-13
#APP_STL := stlport_static
APP_STL := gnustl_static
APP_OPTM := release
APP_CPPFLAGS += -std=c++11 -fexceptions -frtti
