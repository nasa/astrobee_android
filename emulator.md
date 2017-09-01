# Android Emulator setup for Astrobee Simulator

Steps to Setup the network between the Astrobee Simulator and the Android
Emulator.

*Note that these instructions allow to experiment with a full Android emulator
and a running Astrobee simulator. However this method is not integrated yet
with the Astrobee ROS Java API described in guest_science\readme.md*


These steps assume a Windows PC with a virtual machine hereafter referred to as
HOST running Ubuntu (16.04) with the Astrobee Simulator installed inside HOST.
 - Note 1: VMWare Workstation Player 12.5 is used because VirtualBox is too slow for this type of application
 - Note 2: These instructions are likely to work from a native Ubuntu install but have not been tested.

Please use the scripts from the `scripts` directory (top-level of `freeflyer_android`).

## 1. Install and configure Android Studio.
  Download Android Studio from [Android's developer homepage](https://developer.android.com/studio/index.html) and extract into your home directory. Run the following in the shell to start the IDE (Integrated Development Environment):

  ```shell
  android-studio/bin/studio.sh
  ```

  Android Studio will proceed to download several other packages and updates. You may wish to add Android Studio's scripts to your PATH variable so that it can be used more easily.

## 2. Add an emulator. 
To do so:
1. Inside Android Studio go to Tools -> Android -> AVD Manager
2. At the AVD (Android Virtual Device) Manager, click the "Create Virtual Device" button.
3. Choose a Nexus 5 phone with a resolution of 1080x1920 xxhdpi as the hardware the AVD will emulate.
4. Select Next, and in the "Select a system image", click on the "x86 Images" tab, and select Marshmallow/API Level 23/ABI x86_64/Android 6.0.
5. Setup up the hardware to be in portrait mode.
6. Click next and leave the default values in the following screen and click Finish.

__*Optional*__ Keep in mind that one may need to increase the heap size in Android Studio. If necessary:
  * Click Help > Edit Custom VM Options to open the `studio.vmoptions` file.
  * Add a line to the `studio.vmoptions` file to set maximum heap size using the syntax `-XmxheapSize`. The size you choose should be based on the size of your project and the available RAM on your machine. As a baseline, if you have more than 4GB of RAM and a medium-sized project, you should set the maximum heap size to 2GB or more. The following line sets the maximum heap size to 2GB: `-Xmx2g`.
  * Save your changes to the `studio.vmoptions` file, and restart Android Studio for your changes to take effect. For more info go [here](​https://developer.android.com/studio/intro/studio-config.html)

## 3. Setup environment variables
Check the internal name that Android Studio gives the emulator by typing and running from the shell
```shell
~/Android/Sdk/tools/emulator -list-avds 
```

From the shell, type and run
```shell
  export EMULATOR=$HOME/Android/Sdk/tools/emulator
  export AVD="insert here name obtained from the previous command".
  # This name may be "Nexus_5_API_23_1"
```

## 4. Running the emulator
In order to setup correctly the communication between the Android emulator and the rest of the simulator we need to run an script that takes care of that.

1. Launch emulator script (which will set the HOST network)
```shell 
./launch_emulator.sh
```

1. Once the emulator is running for the first time, setup the emulator’s network inside its cache. From the HOST (Linux) prompt:
```shell
$HOME/Android/Sdk/platform-tools/./adb push ~/<PathToTheScript>/emulator_setup-net.sh /cache
# gets into a root shell, but only on non-google-API images
$HOME/Android/Sdk/platform-tools/adb shell
```

1. Inside the emulator’s shell, run the emulator setup script (copied to cache
   in the previous step)
```shell
sh /cache/emulator_setup_net.sh
```

1. Ping the EMULATOR from the HOST and vice versa to check everything went OK

1. Make sure we sourced our ROS environment
```shell
#From the location where the "freeflyer_build" folder is:
~/freeflyer_build$ . devel/setup.bash
```

1. Run the Astrobee Simulator with the following command from the prompt:
```shell
ROS_HOSTNAME=10.0.42.1 roslaunch astrobee sim.launch rviz:=true sviz:=true
```
the last two flags determine if the 3D visualizations of the robot (RViz and Gazebo) are run or not.

1. Publish and subscribe to topics using only ROS, to verify proper
   communication between the emulator and the host

1. Publish and subscribe to topics using the Astrobee simulator, to verify proper communication between the emulator and the host

## 5. Running the sample Guest Science App
At this point, it is assumed that you are running the ROS-core simulator using the previously explained procedure, Android Studio IDE is running, and you launched the Android emulator using the previously explained procedure (so that the communication between the ROS-core simulator and the Android emulator is guaranteed). 
Currently, the sample app requires to manually run the Android-ROS bridge service prior to running the Guest Science sample app. 

To run the Android-ROS bridge:
1.  From Android Studio, open the "android_ros_bridge" project.
2.  From the left frame of the Android Studio IDE, click on Project and navigate through the “java” folder and click on the first folder.
3.  Double-click on the MainActivity java file.
4.  You can run the program by clicking the green triangle “Run” button or press “Shift+F10”.
5.  The emulator will show up and the service's GUI will initiate. At this point a notification "Service Started" should pop-up.

With the Android-ROS bridge service running, let's run the Guest Science example app:
1. From Android Studio, open the “GuestScienceActivity” project.
2. From the left frame of the Android Studio IDE, click on Project and navigate through the “java” folder and click on the first folder.
3. Double-click on the three files: MainActivity, GuestScienceSampleApp, and RobotCommands.
4. You can edit the GuestScienceSampleApp class to program the robot to do your experiment. For now become familiar with this class and see how it works: it creates a list of high-level commands the robot can run in the simulator and their corresponding arguments (if required).
5. You can run the program by clicking the green triangle “Run” button or press “Shift+F10”.
6. The emulator will show up and the activity will initiate. Bring the RViz or Gazebo window to focus to observe what Astrobee will do.
7. In the emulator’s window, press the “Bind to service” button. This will initiate the communication between the GuestScience activity (representing what the HLP would host) and the ROS-core based simulator (representing the vehicle and its 3D representation) and will also initiate the sending of the commands in the list specified in GuestScienceSampleApp.
