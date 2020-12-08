# Android Science Camera Image (sci_cam_image)

This is a Guest Science Android application that takes full-resolution
pictures with the science camera.

The pictures are published on the `/hw/cam_sci/compressed` topic via
ROS at reduced resolution (640x480). The image dimensions (at the same
resolution) and other metadata are published on `/hw/cam_sci_info`.

The full-resolution images (at 5344x4008 pixels) are saved locally on
HLP in directory:

  `/sdcard/data/gov.nasa.arc.irg.astrobee.sci_cam_image/delayed`

and can be fetched later.

This app has no GUI. The user should use it via the guest science
manager.

## Setting up the environment

Set the variables that point to your local copy of the astrobee main
repository and to the astrobee android repository, for example, as:

    export SOURCE_PATH=$HOME/astrobee
    export ANDROID_PATH=$SOURCE_PATH/submodules/android

## Building the app

It is expected that you will have read `$ANDROID_PATH/build_essential_apks.md`
and `$ANDROID_PATH/running_gs_app.md` for background information.

Run on your development machine:

    cd $ANDROID_PATH/gs_examples/sci_cam_image
    ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug

Copy the obtained APK to the LLP processor of the robot, for example, as:

    rsync -avzP app/build/outputs/apk/app-debug.apk bsharp-llp:sci_cam_image.apk

## Setting up some tools on LLP

Run:

    rsync -avzP $ANDROID_PATH/scripts/gs_manager.sh       \
      $SOURCE_PATH/tools/gds_helper/src/gds_simulator.py  \
      bsharp-llp:

to copy these two tools to the home directory on LLP.

## Ensure the robot has the correct time

Connect to LLP and run the `date` command. From there, connect to HLP,
via the:

    adb shell

command and run the `date` command as well. If these do not agree, that
will be a serious problem and must be fixed before continuing.

## Setting up ROS communication

This app assumes sets the ROS master URI as

    http://llp:11311

If that is not the case, the code must be edited to set the correct
value and rebuilt. You may need to also set the environmental variable
`ROS_MASTER_URI` to the same value in any shell that is used to do ROS
communication.

## Installing the sci_cam_image APK

Connect to LLP. Run:

    adb uninstall gov.nasa.arc.irg.astrobee.sci_cam_image
    adb install -g sci_cam_image.apk

(This will replace any older version of this app.)

## Running this APK using the Guest Science Manager

Connect to LLP in several terminals. In one, start the ROS nodes on
the bot:

    roslaunch astrobee astrobee.launch mlp:=mlp llp:=llp

In a second session on LLP start the Guest Science Manager. Here we
will use the command-line tools rather than the GDS GUI program, when
the instructions would be a little different.

Run:

    ./gs_manager.sh start

followed by starting the command-line GDS tool:

    python ./gds_simulator.py

and follow the prompts. This APK figures as SciCamImage in the
list. Choose it from the list, and start it by pressing on 'b'. By
default, it will not take pictures unless told so by subsequent
commands.  The app can be stopped by pressing on 'c'. To control it,
press on 'd', to be able to send custom science commands. Then, the
following dialog will come up.

    1)  Take a single picture
            {"name": "takeSinglePicture"}
    2)  Turn on continuous picture-taking
            {"name": "turnOnContinuousPictureTaking"}
    3)  Turn off continuous picture-taking
            {"name": "turnOffContinuousPictureTaking"}
    4)  Turn on saving pictures to disk
            {"name": "turnOnSavingPicturesToDisk"}
    5)  Turn off saving pictures to disk
            {"name": "turnOffSavingPicturesToDisk"}
    6)  Set preview image width
            {"name": "setPreviewImageWidth", "value": 640}
    7)  Set focus distance
            {"name": "setFocusDistance", "value": 0.39}
    8)  Set focus mode
            {"name": "setFocusMode", "value": "manual"}
    9)  Set image type
            {"name": "setImageType", "value": "color"}
    10)  Exit program

The user can either choose a number, to send the specified command,
or type manually the command, which is the text starting and ending
with braces, and editing it appropriately. The latter is necessary
for the options that set a value.

After a guest science command is issued, press 'Enter' a couple of
times to go to the previous dialog.

To quit the guest science manager (after stopping `SciCamImage` and
exiting the simulator), do:

    ./gs_manager.sh stop

If the guest science manager is not behaving, one can use the option

    ./gs_manager.sh  hard_stop

## Description of the custom guest science commands

1. Take a single picture

    Take a single picture. It will be published via ROS as a preview image
    of dimensions 640x480 pixels (this is customizable, see below), and it
    will be will be saved to disk on HLP at full resolution.

    If continuous picture taking is on before this option is invoked (see
    below), continuous picture taking will be stopped, and then just one
    picture will be taken.

2. Turn on continuous picture-taking

    Enable taking and publishing pictures as fast as the camera will allow
    it (typically one image per a second or two). By default this
    functionality is off.

3. Turn off continuous picture-taking

    Turn off the above functionality.

4. Turn on saving pictures to disk

    Allow the pictures to be written to disk on HLP at full
    resolution. The directory having them is mentioned earlier. This
    is the default.

5. Turn off saving pictures to disk

    The opposite of the above option.

6. Set preview image width

    Set the preview image width for publishing over ROS. The default is
    640 pixels. If set to 0, it will result in preview images being
    published at full resolution.

7. Set focus distance

    Set the focus distance. The default is 0.39 (in units of 1/m)
    corresponding to the plane of best focus being approximately 1 m from
    the robot body. A value of 0 will result in the focus being set at
    infinity.

8. Set focus mode

    The default focus mode is 'manual', with the focus distance specified
    earlier. This can be set to 'auto', when it will do auto-focus.

9. Set image type

    Set the output image type as one of 'color' or 'grayscale' for
    both preview mode and saving to disk. The default is 'color'.
    It is important to note that, while counterintiutive, grayscale
    images take up more than twice the storage of color images. Until
    further study this option is discouraged.
 
10. Exit program

    This will quit gds_simulator.py, without quitting the sci cam image
    app. To quit the app, go to the previous screen (hit enter) and
    choose the appropriate option.

    Don't forget to exit the guest science manager as well.

## Logging with adb

To see logging info as this app is running one can do (in a separate
terminal on LLP):

    adb logcat -b all -c   # wipe any existing logs
    adb logcat adb logcat -s sci_cam

For more verbose logging one can do:

    adb logcat | grep -E -i "sci_cam|science"

## Running this APK in debug mode

To investigate any problems with this APK it can be run without the
Guest Science Manager and without the full set of astrobee ROS
software. One should also turn on logging for this apk.

For that, in one terminal on LLP launch `roscore`, then in a second
one run logging as earlier, and in a third one run:

   adb shell am startservice -n gov.nasa.arc.irg.astrobee.sci_cam_image/.SciCamImage

After this, one of the following self-explanatory commands can be sent
to the sci cam, paralleling the ones described earlier.

    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.TAKE_SINGLE_PICTURE

    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_ON_CONTINUOUS_PICTURE_TAKING

    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_OFF_CONTINUOUS_PICTURE_TAKING
    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_ON_SAVING_PICTURES_TO_DISK

    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_OFF_SAVING_PICTURES_TO_DISK
    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.SET_PREVIEW_IMAGE_WIDTH --es preview_image_width 640

    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.SET_FOCUS_DISTANCE --es focus_distance 0.39

    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.SET_FOCUS_MODE --es focus_mode manual

    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.SET_IMAGE_TYPE --es image_type color

    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.STOP

In addition, turn on and off more verbose logging with:

    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_ON_LOGGING
    adb shell am broadcast -a gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_OFF_LOGGING

To see if any images are being published one can use rviz to display
the image topic (see above) or just echo the camera info:

    rostopic echo /hw/cam_sci_info

In cases when the sci cam refuses to quit, the app should be
uninstalled (see above), which will force it to stop.
