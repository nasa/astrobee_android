# Android Emulator setup for Astrobee Simulator

Steps to Setup the network between the Astrobee Simulator and the Android
Emulator.

*Note that these instructions allow to experiment with a full Android emulator
and a running Astrobee simulator. However this method is not integrated yet
with the Astrobee ROS Java API described in guest_science\readmed.md*


These steps assume a Windows PC with a virtual machine hereafter referred to as
HOST running Ubuntu (16.04) with the Astrobee Simulator installed inside HOST.
 - Note 1: VMware is used because VirtualBox is too slow for this type of application
 - Note 2: These instructions are likely to work from a native Ubuntu install but have not been tested

Please use the scripts from the `scripts` directory (top-level of `freeflyer_android`).

1. Install and configure Android Studio

  -  For instance, the emulator could be the Nexus_5_API_22. Keep in mind that
     one may need to increase the heap size in Android Studio. If necessary:

    - click Help > Edit Custom VM Options to open the `studio.vmoptions` file.
    - Add a line to the `studio.vmoptions` file to set maximum heap size using
    the syntax `-XmxheapSize`. The size you choose should be based on the size
    of your project and the available RAM on your machine. As a baseline, if you
    have more than 4GB of RAM and a medium-sized project, you should set the
    maximum heap size to 2GB or more. The following line sets the maximum heap
    size to 2GB: `-Xmx2g`.
    - Save your changes to the `studio.vmoptions` file, and restart Android
      Studio for your changes to take effect. For more info:
      ​https://developer.android.com/studio/intro/studio-config.html

1. Missing steps
  - Add a virtual device
  - Configure the virtual device (right API level, screen resolution, etc.)

1. Setup environment variables
```
  export EMULATOR=$HOME/Android/Sdk/tools/emulator
  export AVD=’name that one gives it to the emulator in Android Studio’.
  # As mentioned above, this name could be ‘Nexus_5_API_22’
```

1. Launch emulator script (which will set the HOST network)
```
./launch_emulator.sh
```

1. Once the emulator is running for the first time, setup the emulator’s network
   inside its cache from the HOST prompt
```
$HOME/Android/Sdk/platform-tools/adb push ~/emulator_setup-net.sh /sdcard
$HOME/Android/Sdk/platform-tools/adb shell
```

1. Inside the emulator’s shell, run the emulator setup script (copied to cache
   in the previous step)
```
su                  # gets into a root shell, but only on non-google-API images
cp /sdcard/emulator_setup_net /cache/           # somewhere we can execute from
sh /cache/emulator_setup_net.sh
```

1. Ping the EMULATOR from the HOST and vice versa to check everything went OK

1. Run the Astrobee Simulator with the following command from the prompt
```
ROS_HOSTNAME=10.0.42.1 roslaunch astrobee simulator.launch gui:=true
```

1. Publish and subscribe to topics using only ROS, to verify proper
   communication between the emulator and the host

1. Publish and subscribe to topics using the Astrobee simulator, to verify proper communication between the emulator and the host
