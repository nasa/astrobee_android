# Android Science Camera Image 2 (sci_cam_image2)

This is a guest science android application that takes full-resolution
pictures with the science camera. It uses the Android camera2 API,
unlike the earlier sci_cam_image app, which is a huge change.

The pictures are published on the

  /hw/cam_sci/compressed

topic via ROS, at full or reduced resolution. The image dimensions,
etc., are published on

  /hw/cam_sci_info

The full-resolution images may be saved locally on HLP, and can be fetched
later. 

This app has a minimal GUI consisting of an image preview window. The
user should start and stop this application remotely via the guest
science manager and not by using its GUI.

## Setting up the environment

Set the varibles that point to your local copy of the astrobee main 
repository and to the astrobee android repository, for example, as:

    export SOURCE_PATH=$HOME/astrobee
    export ANDROID_PATH=$SOURCE_PATH/submodules/android

## Building the app

It is expected that you will have read $ANDROID_PATH/build_essential_apks.md 
and $ANDROID_PATH/running_gs_app.md for background information.

Run on your development machine:

  cd $ANDROID_PATH/gs_examples/sci_cam_image2
  ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug

Copy the obtained APK to the LLP processor of the robot, for example, as:

  rsync -avzP app/build/outputs/apk/app-debug.apk bsharp-llp:sci_cam_image2.apk

## Setting up some tools on LLP

Copy 

  $ANDROID_PATH/scripts/gs_manager.sh 

and

  $SOURCE_PATH/tools/gds_helper/src/gds_simulator.py

to the home directory on LLP.

## Ensure the robot has the correct time

Connect to LLP and run the 'date' command. From there, connect to HLP,
via the

  adb shell

command and run the 'date' command as well. If these do not agree, that
will be a serious problem and must be fixed before continuiing.

## Setting up ROS communication

This app assumes sets the ROS master URI as 

  http://llp:11311 

If that is not the case, the code must be edited to set the correct value
and rebuilt. You may need to also set the environmental variable
ROS_MASTER_URI to the same value in any shell that is used to do ROS
communication.

## Installing the sci_cam_image2 APK

Connect to LLP. Run:

  adb uninstall gov.nasa.arc.irg.astrobee.sci_cam_image2
  adb install -g sci_cam_image2.apk

(This will replace any older version of this app.)

## Running this APK using the Guest Science Manager.

Connect to LLP in several terminals. In one, start the ROS nodes on the bot:

  roslaunch astrobee astrobee.launch mlp:=mlp llp:=llp

In a second session on LLP start the Guest Science Manager as:

  ./gs_manager.sh start

followed by starting the command-line GDS tool:

  python ./gds_simulator.py

and follow the prompts. This APK figures as SciCamImage2 in the
list. Starting it (press on 'b') will turn on publishing the science
camera images. Yet, no pictures will be taken. 

To take a picture, press on option 'd' to send a custom science
command, and then choose '1' to take a single picture. If desired to
turn on continuous picture taking, which will take and publish
pictures as fast as the camera will allow it (typically one per
second), send another custom science command, and this time choose the
value '2'. To turn off continuous picture taking follow the same
approach, but choose instead '3'. If continuous picture taking is on,
and the user chooses to take one picture only, continuous picture
taking will be stopped, and then just one picture will be taken.

To quit the guest science manager (after stopping SciCamImage2 and
exiting the simulator), do:

  ./gs_manager.sh stop

If the guest science manager is not behaving, one can use the option

  ./gs_manager.sh  hard_stop

To see logging info as this app is running one can do (in a separate
terminal on LLP):

  adb logcat | grep -E -i "science|sci_cam"

## Running this APK in debug mode. 

To investigate any problems with this APK it can be run without the
Guest Science Manager and the full set of astrobee ROS software. One
should also turn on logging for this apk. 

For that, in one terminal on LLP launch 'roscore', then in a second
one run:

  adb logcat -b all -c   # wipe any existing logs
  adb logcat -s sci_cam  # print log messages for sci_cam_image2

and in a third one run:

  adb shell am start -n gov.nasa.arc.irg.astrobee.sci_cam_image2/gov.nasa.arc.irg.astrobee.sci_cam_image2.SciCamImage2

After this, one of the following self-explanatory commands can be sent
to the sci cam:

  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.TAKE_SINGLE_PICTURE
  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_ON_CONTINUOUS_PICTURE_TAKING
  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_OFF_CONTINUOUS_PICTURE_TAKING
  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_ON_LOGGING
  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_OFF_LOGGING
  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_ON_SAVING_PICTURES_TO_DISK
  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_OFF_SAVING_PICTURES_TO_DISK
  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.STOP

There exist several other commands that can set a specific
parameter. Here are some examples.

Set the focus distance:

  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.SET_FOCUS_DISTANCE --es focus_distance 0.39

Here, the focus distance can be any nonnegative number. It is measured
in units of 1/m, so a value of 0 is focus at infinity, and the bigger
it is the closer the camera focuses.

The value 0.39 is what gives best manual focus for objects 1 m away
from the robot and is the default value for this app. Note that the
camera has only a few acceptable values for the focus distance, so
values set by the user are adjusted to the nearest value it can
accept.

Set the focus mode, to either manual (the default) or to auto (that
is, auto-focus).

  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.SET_FOCUS_MODE --es focus_mode manual

See the width, in pixels, for the preview images published over ROS:

  adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image2.SET_PREVIEW_IMAGE_WIDTH --es preview_image_width 1024

If not specified or if set to 0, the full resolution will be used. The
images written to disk locally are, however, always written at full
resolution.

To see if any images are being published one can use rviz to display 
the image topic (see above) or just echo the camera info:

  rostopic echo /hw/cam_sci_info

In cases when the sci cam refuses to quit, the app should be
uninstalled (see above), which will force it to stop.