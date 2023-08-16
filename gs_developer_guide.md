# Guest Science Developer Guide

This guide assumes you have read and understood the concepts in the
[Guest Science Readme](guest_science_readme.md). It also assumes you are familar
with Java and IDEs.

## 1. Getting Started with Guest Science

Guest Science APKs run on the HLP. Most guest scientists will not have access
to a HLP board for APK development. Thus, the Astrobee team supports 3 ways
guest scientists can develop their APKs: java only, an Android emulator, or a
HLP development board. If you don't have a development board, we recommend
following the [Android emulator](#1.1.-android-emulator) instructions as an
Android emulator will be the closest thing to running on the robot. However the
emulator can be resource intensive. So if you do not have a high performance
machine or if you want simpler/quicker development (i.e. don't want to learn
Android), we recommend using our java only framework. If you use java only,
please skip down to the
[running a guest science application](#2.-running-a-guest-science-application)
section. If you are lucky and have an HLP board, we recommend following the
[HLP development board](#1.2.-hlp-development-board) instructions.

### 1.1. Install and Configure Android Studio

Please download Android Studio, it's recommended that you download version 4.1.2 from
[Android Studio's archives](https://developer.android.com/studio/archive),
and extract it into your home directory. Run the following line in the shell to 
start the IDE (Integrated Development Environment):

  ```shell
  android-studio/bin/studio.sh
  ```

Android Studio will proceed to download several other packages and updates.
You may wish to add Android Studio's scripts to your PATH variable so that it
can be used more easily.

### 1.2. Android Emulator

If you don't have Android Studio, please see 1.1.

To setup a full simulation environment with an Android emulator, follow the
[emulator](emulator.md) instructions.

### 1.3. HLP Development Board

If you don't have Android Studio, please see 1.1.

Coming soon!!!

## 2. Running a Guest Science Application

Please see the [running a guest science application](running_gs_app.md)
documentation for how to run a guest science example.

## 3. Creating a Guest Science Application

Please see the [creating a guest science application](creating_gs_app.md)
documentation for how to set up your own guest science APK.

