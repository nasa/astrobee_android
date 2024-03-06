# Running a Guest Science Application

This readme assumes you have followed the 
[Guest Science Developer Guide](gs_developer_guide.md) and the
[`astrobee/simulation/readme.md`](https://github.com/nasa/astrobee/blob/master/simulation/readme.md).

It also assumes that `astrobee_android` is checked out in `$ANDROID_PATH`, and
`astrobee` source is checked out in `$SOURCE_PATH`.
            
This readme teaches you how to run a Guest Science Application by running one of
the Guest Science examples. Once you have your own Guest Science Application, it
should be trival to use these instructions and replace the example name with
your project's name. Please follow all four sections. However, if you are doing
Java only development, please use the Java only subsections and if you are using
the emulator or HLP board, please use the Android subsections.

## 1. Setup

In every terminal you use, be sure to set up your environment. If you forgot how
to do this, follow the Setting up your Environment section of the
[`astrobee/simulation/running_the_sim.md`](https://github.com/nasa/astrobee/blob/master/simulation/running_the_sim.md).

### Java Only

Coming soon!!!


### Android

Please start your emulator or HLP board and make sure you can communicate with
it (adb and network i.e. ping). Please refer to the [emulator](emulator.md) or
the [HLP board](hlp_devkit_install.md) instructions if you forget how to do
this.

#### Install JDK 8 and set up JAVA_PATH
```shell
  sudo apt install openjdk-8-jdk
  # Check the location of where Java is installed
  whereis java
  export JAVA_PATH="insert here the path obtained from the previous command"

```

#### Build and Install the Guest Science Manager
```shell
  cd $ANDROID_PATH/core_apks/guest_science_manager
  ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug
  adb install -g -r activity/build/outputs/apk/activity-debug.apk
```
**Important** Please make sure to type the `g` before `r`. If you don't, Android
will not grant the APK the right permissions and the APK will fail to excute.


#### Build and Install the Test Simple Trajectory Example

You will be running the test simple trajectory example. This example undocks
the Astrobee and then moves it in a rectangular shape.
```shell
  cd $ANDROID_PATH/gs_examples/test_simple_trajectory
  ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug
  adb install -g -r app/build/outputs/apk/app-debug.apk
```
**Important** Please make sure to type the `g` before `r`. If you don't, Android
will not grant the APK the right permissions and the APK will fail to excute.

#### Set ROS Environment Variables

Please open two terminals and do the following in both terminals:
```shell
  export ROS_IP=$(getent hosts llp | awk '{ print $1 }')
  export ROS_MASTER_URI=http://${ROS_IP}:11311
  echo $ROS_IP
```
Ensure you can ping/connect to the ip echoed.

When you get to section 2 (Run the Simulator), use terminal 1. When you get to
section 4 (Guest Science Commanding), use terminal 2.

## 2. Run the Simulator

Please start the simulator. If you forgot how to do this, follow the Running
the Simulator section of the
[`astrobee/simulation/running_the_sim.md`](https://github.com/nasa/astrobee/blob/master/simulation/running_the_sim.md).


## 3. Start the Guest Science Manager

### Java Only

Coming soon!!!

### Android
```shell
  # Once you use 'hard_stop' to close the gs_manager, you will have to use 'restart' afterwards
  # instead of 'start' if you want to use the gs_manger again. And so, for convenience,
  # it's easier to use 'restart'. 
  $ANDROID_PATH/scripts/gs_manager.sh restart
```
### 4. Guest Science Commanding
```shell
  cd $SOURCE_PATH/tools/gds_helper/src
  python gds_simulator.py
```
If you encounter an issue running the GDS simulator, then you 
may need to update the file. Depending on your environment
you may use the following command `git checkout develop -- gds_simulator.py`.

The GDS simulator is interactive. It will prompt you for the next step.

1. Press any key to grab control
2. Select the Guest Science Application (GSA) you are trying to run
3. Type `b` and press `Enter` to start the GSA
4. Press `Enter` to stop listening for data
5. Press any key to get back to the application menu

If you want to see the robot move, command it to do so:

1. Type `d` and press `Enter` to send a custom guest science command
2. Type `1` and press `Enter` to run the trajectory

Now Astrobee will undock and move in a rectangle.

### 5. Stop 

In the terminal running the GDS simulator, please do the following:

1. If needed, press `Enter` to stop listening for data
2. If needed, press any key to get back to the application menu
3. Type `c` and press `Enter` to stop the GSA
4. Press `Enter` to stop listening for data
5. Press any key to get back to the application menu
6. Type `f` and press `Enter` to exit the GDS simulator

In the terminal running the simulator, enter `Ctrl+c`

#### Java Only

Coming soon!!!

#### Android
```shell
$ANDROID_PATH/scripts/gs_manager.sh hard_stop
```

To close the emulator, click on the `x` symbol.
