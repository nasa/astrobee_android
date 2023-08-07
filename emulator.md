# Android Emulator setup for Astrobee Simulator

Steps to set up an Android Emulator and the network between the Astrobee
Simulator and the Android Emulator.

These steps assume you are running Ubuntu (16.04 or 20.04) either natively or in a 
virtual machine with the Astrobee Robot Software installed. This Ubuntu instance will be referred to as `HOST`.
 - Note 1: Astrobee settings require 3 IP addresses (LLP, MLP, HLP). Make sure
     to choose a set of IPs that fits your needs (10.42.0.34-36 is commonly used).
     Ensure you keep the same IPs when setting Ubuntu and Android network and
     when running the simulator. In this setup LLP correspond to the HOST (Ubuntu)
     and HLP to the emulated device (MLP is not used but should be included to keep
     standards).
 - Note 2: **Important**. Make sure you don't have an Android device connected to the HOST before starting this process.

Please use the scripts from the `scripts` directory (top-level of the android
repository).

## 1. Start Android Studio

  ```shell
  android-studio/bin/studio.sh
  ```

## 2. Add an emulator

To do so:
1. If you have a project open go to File -> Close Project, to return to the Android Studio welcome screen. Otherwise, on the bottom right of the page, you should see and go to Configure -> AVD Manager.
    - Note: You may need to expand the window if you don't already see Configure and Get Help on the bottom right.
2. In the AVD (Android Virtual Device) Manager window, click the "Create Virtual
   Device" button.
3. Choose a Nexus 5 phone with a resolution of 1080x1920 xxhdpi as the hardware
   the AVD will emulate.
4. Select Next, and in the "Select a system image", click on the "x86 Images"
   tab, and select Nougat/API Level 25/ABI x86_64/Android 7.1.1 (NO Google APIs).
   Download it if needed.
    - Note: If you using a virtual machine and receive the message, "Your CPU does not support required features (VT-x or SVM)," then you will need to enable Nested VT-x/AMD-V for your VM.
5. Setup up the hardware to be in portrait mode.
6. **Optional**. Click on _Show Advanced Settings_. Scroll down and edit
   `Memory and storage` to higher values for better performance. Consider change
   the RAM and VM Heap to something greater than 1.5 GB.
7. Click Finish
8. Close the AVD Manager window and Android Studio.

## 3. Install ADB
ADB (Android Debug Bridge) allows the user to access physical and emulated
Android devices, push/pull files, and manage applications. Android Studio
already includes this program. However, it is important you install it as a
separate package. In a terminal, please do the following:

```shell
sudo apt-get install adb
```

## 4. Setting HOST network

### 4.1. Edit HOSTS file

To do so:
1. Open the hosts file with an editor of your election (we will use nano since
it comes with Linux distributions)

```shell
sudo nano /etc/hosts
```

2. Add 3 lines (from line 4 to line 6). Substitute <x_ip> for a valid unique IP.
For example, `<hlp_ip>	hlp` becomes `10.42.0.36	hlp`.

```shell
127.0.0.1	localhost
127.0.1.1	ubuntu

<hlp_ip>	hlp
<mlp_ip>	mlp
<llp_ip>	llp

[...]
```

3. Save and close file

### 4.2. Setup environment variables

From the shell, type and run
```shell
  # You may want to add these variables to your bashrc file.

  # If you are using a standalone repo, the path probably will be:
  #     $HOME/astrobee_android
  # If you are using this repo as a submodule, the path may be:
  #     $HOME/astrobee/src/submodules/android
  export ANDROID_PATH="insert here the path to android repository"

  # Location of emulator executable file. You may have a different path depending
  # on your installation process. Also depending on your version of Android Studio,
  # instead of tools/emulator it may be emulator/emulator
  export EMULATOR=$HOME/Android/Sdk/tools/emulator

  # Check the internal name that Android Studio gives the emulator by typing and
  # running from the shell
  $EMULATOR -list-avds

  # This name may be "Nexus_5_API_25"
  export AVD="insert here name obtained from the previous command"

```

### 4.3. Setting network bridge and running the emulator

In order to correctly set up the communication between the Android emulator and
the rest of the simulator, we need to run a script that takes care of that.

1. Launch the emulator script which will set the HOST network. Provide your
super user password if requested.

```shell
cd $ANDROID_PATH/scripts
./launch_emulator.sh
```

## 5. Setting Android network

1. Using **another terminal** from the HOST (Ubuntu), pull the Android hosts
file to your home directory.

```shell
  # Set variable again since it is a new terminal, unless you add it to basrc
  export ANDROID_PATH="insert here the path to android repository"

  adb pull /system/etc/hosts $HOME
```

2. Open the file located in `$HOME/hosts`. Add the following text and save it.
Substitute <x_ip> for a valid unique IP (you may use `nano $HOME/hosts`). **Reminder**:
Ensure you keep the same IPs when setting HOST (Ubuntu) and Android network and when running the simulator.

```shell
  127.0.0.1       localhost
  ::1             ip6-localhost

  # Add the following three lines and replace <x_ip> for a valid unique IP
  <hlp_ip>        hlp
  <mlp_ip>        mlp
  <llp_ip>        llp

```

3. Push the file to the Android Emulated Device.

```shell
adb root	# Wait a few seconds
adb remount
adb push ~/hosts /system/etc
```

4. Open the file located in `$ANDROID_PATH/scripts/emulator_setup_net.sh`
and edit the following line:

```shell
  # Change this IP for your HLP IP.
  # Make sure it matches both hosts files (Ubuntu, Android).
  ip addr add 10.42.0.36/24 dev eth1
```

5. Save your changes and push the file to the emulated device.
From the HOST (Ubuntu) prompt:

```shell
  adb push $ANDROID_PATH/scripts/emulator_setup_net.sh /cache
```

6. Execute the previous file inside the Android device.

```shell
  adb shell su 0 sh /cache/emulator_setup_net.sh
```

7. Execute `ping llp` and `ping hlp` from Android and Ubuntu to make sure the
network is up and running.

```shell
  # test Android Network to itself (hlp) and HOST (llp)
  adb shell ping -c3 hlp
  adb shell ping -c3 llp
  # test HOST Network to itself (llp) and Android (hlp)
  ping -c3 llp
  ping -c3 hlp
```

## Additional Important Information

 - To close the emulator, click on the `x` symbol.

 - To run the emulator again execute the following lines from the HOST:

   ```shell
   # Set if not already exported
   export ANDROID_PATH="insert here the path to android repository"
   export EMULATOR=$HOME/Android/Sdk/tools/emulator
   export AVD="insert here name obtained from the previous command"
   cd $ANDROID_PATH/scripts
   ./launch_emulator.sh
   ```

 - IP network configuration is NOT persistent. Once you run the emulator again,
   you will have to set the network again by performing step `5.6`.

 - You can also let the script handle the network for you. To do so, run the
   emulator as follows:

   ```shell
   # Set if not already exported
   export ANDROID_PATH="insert here the path to android repository"
   export EMULATOR=$HOME/Android/Sdk/tools/emulator
   export AVD="insert here name obtained from the previous command"
   cd $ANDROID_PATH/scripts

   # Flag -n will run an additional script that will handle the emulator
   # network for you. It will read your HOSTS file in order to set the IPs.
   # If you want to override these IPs, you may export `LLP_IP` and `HLP_IP`.
   #  - Note: Running with -n may result in lower performance or a longer startup time. 
   ./launch_emulator.sh -n
   ```

 - Please note `launch_emulator.sh` will perform a Cold Boot every time and it
   will not save any state at the end. It will also run the system partition as
   writable.

 - Please be advised not to run more than one Android device at a time, since
   this may cause a conflict of IPs.

 - If you happen to run the emulator by any other means besides the
   `launch_emulator.sh` script, please be advised your hosts file may be wiped
   away. You may also encounter performance issues.
