#!/bin/sh

dir=$(dirname "$(readlink -f "$0")")
. "$dir"/common.sh || { echo >&2 "something is really messed up"; exit 1; }

toolchain_dir="$wd/toolchain"
cache_dir="$wd/cache"

sdk="sdk-tools-linux-4333796.zip"
sdk_checksum="92ffee5a1d98d856634e8b71132e8a95d96c83a63fde1099be3d86df3106def9"

if [ ! -d "$cache_dir" ]; then
  mkdir -p "$cache_dir"
fi

if [ ! -d "$toolchain_dir" ]; then
  mkdir -p "$toolchain_dir"
fi

# Download SDK
if [ ! -d "$toolchain_dir/sdk" ]; then
  download "https://dl.google.com/android/repository/$sdk" "$sdk"
  echo "$sdk_checksum $wd/cache/$sdk" | sha256sum -c -w || {
    rm "$cache_dir/$sdk"
    die "Checksum for sdk failed, try running the script again"
  }

  unzip "$cache_dir/$sdk" -d "$toolchain_dir/sdk" || \
    die "Unable to unzip sdk"
fi

cd "$toolchain_dir/sdk/tools/bin" || {
  rm -rf "$toolchain_dir/sdk"
  die "Unable to cd into sdk folder"
}

if [ ! -d "$toolchain_dir/sdk/build_tools/25.0.3" ]; then
  ./sdkmanager "build-tools;25.0.3" || die "Unable to install build tools"
fi

if [ ! -d "$toolchain_dir/sdk/platforms/android-25" ]; then
  touch ~/.android/repositories.cfg || die "~/.android folder not created"
  ./sdkmanager "platforms;android-25" || die "Unable to install platform"
fi;

if [ ! -d "$toolchain_dir/sdk/platform-tools" ]; then
  ./sdkmanager "platform-tools" || die "Unable to install platform tools"
fi

if [ ! -d "$toolchain_dir/sdk/ndk-bundle" ]; then
  ./sdkmanager "ndk-bundle" || die "Unable to install ndk"
fi

if [ -d "$toolchain_dir/sdk/ndk-bundle/toolchains" ]; then
  cd "$toolchain_dir/sdk/ndk-bundle/toolchains"
  if [ ! -e mips64el-linux-android ]; then
    ln -s aarch64-linux-android-4.9 mips64el-linux-android
  fi

  if [ ! -e mipsel-linux-android ]; then
    ln -s arm-linux-androideabi-4.9 mipsel-linux-android
  fi
fi

cd "$wd"
