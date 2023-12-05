# Science Camera Image APK

This is an Android guest science application that takes full-resolution
pictures with the science camera.

The pictures are published on the `/hw/cam_sci/compressed` topic via
ROS at reduced resolution (default: 640x480). The image dimensions for
the published image is published on the `/hw/cam_sci_info` topic. This message
is published every time an image is taken regardless of if the image is
published or not and it contains the timestamp the image was taken.

The full-resolution images (at 5344x4008 pixels) are saved locally on
HLP in directory:

  `/sdcard/data/gov.nasa.arc.irg.astrobee.sci_cam_image/delayed`

and can be fetched later.

This app has no GUI. It should be invoked via the guest science
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

    cd $ANDROID_PATH/guest_science_projects/sci_cam_image
    ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug

After building the apk, you will probably want to copy it to the robot. Please
copy it to the LLP processor. For example:

    rsync -avzP app/build/outputs/apk/app-debug.apk bsharp-llp:sci_cam_image.apk

## Ensure the robot has the correct time

Connect to LLP and run the `date` command. From there, connect to HLP,
via the:

    adb shell

command and run the `date` command as well. If these do not agree, please sync
the processors. To do this, run the following command on astrobeast or computer:

    $(date '+%s.%N')

Then pass the result to the set time script. This must be run on the MLP:

    /opt/astrobee/ops/set_time.bash %s.%N

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

First, connect to LLP. Then start the Astrobee fsw by running: 

    systemctl --user start fsw

Next, start the guest science manager by running:

    /opt/astrobee/ops/gs_manager.sh start

Now use the gds helper to start the science camera image apk, issue
commands to the apk, and stop it. First navigate to the helper:

    cd /opt/astrobee/ops/gds/

It is recommended to use the gds helper GUI as it shows feedback for the
commands but all of the following commands can be used on the command line with
the batch gds helper. To start the GDS helper, run:

    python gds_helper.py

To start the apk, run this line in the gds helper:

    gs -start SciCamImage

Once the science camera image apk is started, you can issue any of the following
commands using the gds helper. For more information on what the commands do,
please see the next section.

    1) gs -cmd SciCamImage '{"name": "takePicture"}'
    2) gs -cmd SciCamImage '{"name": "setAutoExposure", "auto": true}'
    3) gs -cmd SciCamImage '{"name": "setContinuousPictureTaking", "continuous": true}'
    4) gs -cmd SciCamImage '{"name": "setFocusDistance", "distance": 0.39}'
    5) gs -cmd SciCamImage '{"name": "setFocusMode", "mode": "manual"}'
    6) gs -cmd SciCamImage '{"name": "setPublishImage", "publish": true}'
    7) gs -cmd SciCamImage '{"name": "setPublishedImageSize", "width": 640, "height": 480}'
    8) gs -cmd SciCamImage '{"name": "setPublishedImageType", "type": "color"}'
    9) gs -cmd SciCamImage '{"name": "setSavePicturesToDisk", "save": true}'
    10) gs -cmd SciCamImage '{"name": "setFocusDistanceFunctionValues", "exponent": -1.41, "coefficient": 1.6}'

To stop the apk, run this line in the gds helper:

    gs -stop SciCamImage

To close the gds helper, please press F10. 

If you want to use the batch gds helper on the command line, simply put one of
the above commands in quotes and give it to the batch helper. For example:

    python gds_helper_batch.py cmd "gs -cmd SciCamImage '{"name": "takePicture"}'"

To stop the guest science manager (after stopping SciCamImage), do:

    ./gs_manager.sh stop

If the guest science manager is not behaving, one can use the option

    ./gs_manager.sh  hard_stop


## Description of the custom guest science commands

1. Take Picture

    Take a single picture. If enabled, it will be published via ROS as a
    preview image of dimensions 640x480 pixels (this is customizable, see
    below), and saved to disk on the HLP at full resolution.

    If continuous picture taking is on while issuing the command, the command
    will be rejected. The command may also be rejected if the apk is in the
    process of capturing an image from a previous take picture command. For
    reference, it takes 1.7 seconds for the apk to capture an image.

2. Set Auto Exposure

    Turns auto exposure on and off. By default, auto exposure is set to true.

3. Set continuous picture-taking

    Enable or disable capture images as fast as the camera will allow
    (typically one image per 1.7 seconds). The captured image will be published 
    via ROS and saved to disk if these options are enabled. By default this
    functionality is disabled.

4. Set focus distance

    Set the focus distance. The default is 0.39 (in units of 1/m)
    corresponding to the plane of best focus being approximately 1 m from
    the robot body. A value of 0 will result in the focus being set at
    infinity.

5. Set focus mode

    Set the focus mode as either 'manual' or 'auto'. By default, it is set to
    'manual', with the focus distance specified earlier.

6. Set publish image

    If this is set to true, the apk will publish captured images over ROS. The
    size and type are configurable with the next two commands. By default, the
    the images are published.

7. Set published image size

    Set the height and width of the images being published over ROS. The default
    size is 640 by 480 pixels.

8. Set published image type

    Set the published image type as 'color' or 'grayscale'. The default is set
    to 'color'.

9. Set save pictures to disk

    If this is set to true, the apk will write captured images to disk on the
    HLP at full resolution. The directory where they are written was mentioned
    in an earlier section. By default, the images are saved to disk.

10. Set focus distance function values

    Modifies the function used to calcute the focal distance based on the haz
    cam distance. It requires a value for both the exponent and coefficient.

## Fetching the data

The sci cam images can be retrieved from HLP with the `adb pull`
command, followed by connecting to HLP with `adb shell` and deleting
the images they are if no longer needed. The ISAAC project provides a
tool to add these images to a bag containing the other robot data
recorded at the same time.

## Logging with adb

To see logging info as this app is running one can do (in a separate
terminal on LLP):

    adb logcat -b all -c   # wipe any existing logs
    adb logcat -s SciCamImage
