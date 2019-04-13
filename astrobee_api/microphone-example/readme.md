# ISAAC microphone demo

In this app Astrobee has a microphone which it uses to navigate aboard
a space station listening to the sound produced by a leak. It publshes
the sound as colored markers that are visualizable in RViz. After an
initial traversal in a lawnmower pattern, it plans and executes a
spiral trajectory around the measurement point encountered so far that
produced the loudest sound to refine the source of the leak.

## Important general notes

* This app does not use the Android development environment which
* should make working with it much easier.
* This app has no GUI.
* This app works by receiving guest science commands from the Ground
  Data System (GDS) and then executing these commands. GDS is the
  workbench for Astrobee.
* This app assumes Astrobee is located in the future Gateway station
  around the Moon.
* The Astrobee is started at point (9.4, -8.2, 6.9).

## Running the example

### Setup environmental variables

The following commands should be used in any terminal. We assume that
`freeflyer_android` is checked out in `$ANDROID_PATH` and that your
Astrobee software is built in `$BUILD_PATH'.  These variables may need
to be adjusted for your specific configuration.

  export BUILD_PATH=$HOME/freeflyer_build/native
  export ANDROID_PATH=$HOME/freeflyer/submodules/android

  source $BUILD_PATH/devel/setup.bash
  export ROS_IP=127.0.0.1
  export ROS_MASTER_URI=http://${ROS_IP}:11311/

### Dealing with ff_msgs

Astrobee uses some custom messages called ff_msgs for communication.
Please follow very carefully the instructions in

    $ANDROID_PATH/astrobee_api/readme.md 

about how to build the ff_msgs jar.

### Building the code

  cd $ANDROID_PATH/astrobee_api
  ./gradlew build

If any errors about ff_msgs are encountered, that means that the
previous step failed and needs to be redone.

### Start the Astrobee simulator

In one terminal, with the above environment variables set, run:

  roslaunch astrobee sim.launch default:=false bumble:=true speed:=0.5 dds:=true \
    gds:=true rviz:=true bumble_pose:="9.4 -8.2 6.9 0 0 0 1"

This will launch GDS to control Astrobee and RViz to see its
trajectory. We will use the Bumble robot, with the above pose.

Once GDS and Rviz loaded fully, in another terminal, with the
environment set up as above, run:

  export ROS_NAMESPACE=bumble
  cd $ANDROID_PATH/astrobee_api
  ./gradlew microphone-example:run

Here, ROS_NAMESPACE is used to set the prefix for the ROS topics
depending on the robot name. This variable must not be set in the
first terminal, as it confuses roslaunch.

### Running the Guest Science Manager

Execute this sequence of actions.

1. Go to the `Teleoperate` tab in GDS. Click on `Grab Control`.

2. Ensure that the `Manual Move Inputs` there agree with the bumble
   pose specified earlier. If it does not, one may click on `Snap
   Preview to Bee` or manually modify those inputs.

3. GDS does not support the Gateway station. Astrobee will show up
   instead outside the ISS. It will display correctly in RViz though.
   In the `Teleoperate` tab please uncheck and apply the options having
   to do with obstacles and keepouts as those won't be accurate for
   Gateway.

4. Go to the `Advanced Guest Science` tab.

5. Check the `Bumble` checkbox. On the same line, ensure that you can
   see and click on the `Microphone` application. If this does not
   work, something failed at previous steps.

6. In the APKs section, choose the `Microphone` app from the dropdown
   menu and click on `Start APK`.

7. In the `Manual Commanding` section, choose the `Microphone` app
   from the APK dropdown menu.

8. Under `Template`, pick one of the actions to run. It can be either
   performing the initial search for leak, or a subsequent refined
   search, or doing both of these at the same time.

9. Switch to RViz. Open the gateway.rviz config file. Ensure that the
   Microphone checkbox is checked.

10. Click on the `Send Command` button in GDS, and then vizualize the
    results in RViz. It may take some time for Astrobee to move.

To echo the messages broadcasted by this app, do

  rostopic echo /bumble/gs/microphone


