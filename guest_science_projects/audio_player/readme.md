# Audio Player

This is an Android guest science application that will play an audio file over
the astrobee speakers. It also publishes a timestamp of when the audio started
playing on the '/gs/audio_player/timestamp' topic.

The audio files that the player plays should be located in:

  `/sdcard/data/gov.nasa.arc.astrobee.audio_player/incoming`

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

    cd $ANDROID_PATH/guest_science_projects/audio_player
    ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug

After building the apk, you will probably want to copy it to the robot. Please
copy it to the LLP processor. For example:

    rsync -avzP app/build/outputs/apk/app-debug.apk bsharp-llp:audio_player.apk

## Setting up ROS communication

This app assumes sets the ROS master URI as

    http://llp:11311

If that is not the case, the code must be edited to set the correct
value and rebuilt. You may need to also set the environmental variable
`ROS_MASTER_URI` to the same value in any shell that is used to do ROS
communication.

## Installing the APK

Connect to LLP. Run:

    adb uninstall gov.nasa.arc.astrobee.audio_player
    adb install -g audio_player.apk

(This will replace any older version of this app.)

## Running this APK using the Guest Science Manager

First, connect to LLP. Then start the Astrobee fsw by running: 

    systemctl --user start fsw

Next, start the guest science manager by running:

    /opt/astrobee/ops/gs_manager.sh start

Now use the gds helper to start the audio player, issue commands, and stop it.
First navigate to the helper:

    cd /opt/astrobee/ops/gds/

It is recommended to use the gds helper GUI as it shows feedback for the
commands but all of the following commands can be used on the command line with
the batch gds helper. To start the GDS helper, run:

    python gds_helper.py

To start the apk, run this line in the gds helper:

    gs -start AudioPlayer

Once the audio player apk is started, you can issue any of the following
commands using the gds helper. For more information on what the commands do,
please see the next section.

    1) gs -cmd AudioPlayer '{"name": "decreaseVolume"}'
    2) gs -cmd AudioPlayer '{"name": "increaseVolume"}'
    3) gs -cmd AudioPlayer '{"name": "playSound", "file": "sound.mp4", "volume": 8, "loop": false}'
    4) gs -cmd AudioPlayer '{"name": "setVolume", "volume": 5}'
    5) gs -cmd AudioPlayer '{"name": "stopSound"}'

To stop the apk, run this line in the gds helper:

    gs -stop AudioPlayer

To close the gds helper, please press F10. 

If you want to use the batch gds helper on the command line, simply put one of
the above commands in quotes and give it to the batch helper. For example:

    python gds_helper_batch.py cmd "gs -cmd AudioPlayer '{"name": "decreaseVolume"}'"

To stop the guest science manager (after stopping AudioPlayer), do:

    ./gs_manager.sh stop

If the guest science manager is not behaving, one can use the option

    ./gs_manager.sh  hard_stop


## Description of the custom guest science commands

1. Decrease Volume

    Decrease the volume by 1. If the volume is 0, it will not be decreased.
    Astrobee's speaker volume ranges from 0 to 15.

2. Increase Volume

    Increase the volume by 1. If the volume is 15, it will not be increased.
    Astrobee's speaker volume ranges from 0 to 15.

3. Play Sound

    The audio player will play the audio file specified and will publish a
    timestamp to ROS of when the audio file started playing. The command takes
    3 parameters: the audio file name, the volume, and looping. The audio
    file name is a required parameter and must be a file in the incoming folder
    for the apk. The volume is an optional parameter and if it is specified, it
    should be between 0 and 15. The looping parameter is also an optional
    parameter and if it is set to true, the audio player will loop the audio
    file until the stop sound command is issued.

4. Set Volume

    Sets the volume of Astrobee's speakers. Takes volume as parameter. If the 
    volume is greater than 15, it will be set to 15 and if the volume is less
    than 0, it will be set to 0.

5. Stop Sound

    Stops the audio file being played. If the player was set to loop the file,
    looping will be stopped.


## Logging with adb

To see logging info as this app is running one can do (in a separate
terminal on LLP):

    adb logcat -b all -c   # wipe any existing logs
    adb logcat -s AudioPlayer
