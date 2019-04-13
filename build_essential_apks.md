# Instructions on building the essential FSW APKs

There are 8 core APKs, 2 test APKs, and 1 example APKs that should be installed
on the HLP before functional testing and launch. The 8 core APKs are the guest
science manager, the disk monitor, the cpu monitor, the signal intention state,
the streaming recorder server, the wifi setup helper, the set wallpaper helper
and set orientation. The test apks are the mic test and the royale viewer and
the example apk is the battery monitor.

The `build_essential_apks.sh` script will build 8 of the core APKs, 1 of the
test APKs, and the example APK. There are 2 APKs that aren't build from this
script -- the set orientation APK and the royale viewer. These 2 APKs can be
found on `volar` in `/home/p-free-flyer/free-flyer/FSW/hlp_files/apks`

Before building the essential APKs, please check `volar` to see if the APKs have
already been built. If they have, you can copy them to your computer and skip to
the install section.

## Running the script

The script assumes that Java is installed. JDK 8 is known to
work with the script. You can install it using:

```sh
you@host:~$ sudo apt install openjdk-8-jdk
```

The script is setup to be similar to the platform creation script. If you have
followed the `readme.md` in platform, you may already have the setup described
below. Please be patient when running the script, it can take 30 minutes to
download the NDK and install necessary SDK tools. Also, you will have to accept
the Android license agreement while executing the script.

The script can generate signed apks or debug apks. If you want the debug version
of the apks, please run the script with no arguments. If you want the signed
version of the apks, please run the script with two arguments; the first
argument should be the key alias and the second argument should be the password
for the keystore. Please see Katie if you don't know what these values are. See
below for examples on how to call the script.

### Setup

`$FFFSW` is assumed to be where the freeflyer software is checked out.

```sh
you@host:~$ B="${FFFSW}"/submodules/android
you@host:~$ mkdir build
you@host:~$ cd build
```

### Building signed apks
The script does use scp to copy the keystore off of volar. Please make sure you
have ssh configured to connect to volar.

```sh
you@host:~/build$ "$B"/scripts/build_essential_apks.sh "my_key_alias" "password"
```

All of the APKs are copied into the `~/build/android/apks/signed` folder.

Maintainer Note: If you aren't ready to copy the executables to a board or if
you are regenerating the apks due to an apk change, you may want to copy them to
`volar` if they aren't already on `volar`. Please make a new folder with the
name YYYYMMDD in the `/home/p-free-flyer/free-flyer/FSW/hlp_files/apks` folder
and copy the apks to that folder. Then copy over the 3 apks in the
`/home/p-free-flyer/free-flyer/FSW/hlp_files/apks` folder into the dated folder
you have created.

### Building debug apks

If you are a guest science developer and you are using these apps on a
development board, you don't need to generate and/or use the signed core apks.
Thus you can run the script as follows.

```sh
you@host:~/build$ "$B"/scripts/build_essential_apks.sh
```

All of the APKs are copied into the `~/build/android/apks/debug` folder.

## Building a single APK

If you only want to build one of the APKs without signing it and have the
android SDK installed, please navigate to the top level directory of the APK you
want to build and do the following:

```sh
apk$ export ANDROID_HOME=/path/to/SDK
apk$ ./gradlew build
```

The built APK will show up in either `app/build/outputs/apk` or
`activity/build/outputs/apk`.

Please note if you used the build essential apks script to install the SDK, your
path to the SDK will be `~/build/android/sdk-linux`.

## Installing APKs

These instructions assume that you have `adb` installed and you can connect to
the HLP through `adb`. It also assumes you are in the `apks` directory. If you
are working on the MLP or LLP, don't forget to copy the `apks` folder to the
processor you are working on. If you are working on the MLP, you will have to
use `adb connect hlp:5555` to connect to the HLP over.

```sh
apks$ adb install -g com.googlecode.eyesfree.setorientation.apk
apks$ adb install -g gov.nasa.arc.astrobee.android.gs.manager.apk
apks$ adb install -g gov.nasa.arc.astrobee.android.sci_cam.apk
apks$ adb install -g gov.nasa.arc.astrobee.cpu_monitor.apk
apks$ adb install -g gov.nasa.arc.astrobee.disk_monitor.apk
apks$ adb install -g gov.nasa.arc.astrobee.set_wallpaper.apk
apks$ adb install -g gov.nasa.arc.astrobee.signal_intention_state.apk
apks$ adb install -g gov.nasa.arc.astrobee.wifisetup.apk
apks$ adb install -g gov.nasa.arc.irg.astrobee.mictest.apk
apks$ adb install -g gov.nasa.arc.irg.astrobee.battery_monitor.apk
apks$ adb install -g org.pmdtec.qtviewer.apk
```
