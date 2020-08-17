# Microphone demo

In this app Astrobee has a microphone which it uses to navigate aboard
a space station listening to the sound produced by a leak. It publishes
the sound as colored markers that can be visualized in RViz. After an
initial traversal in a lawnmower pattern, it can plan and execute a
spiral trajectory around the measurement point encountered so far that
produced the loudest sound to refine the source of the leak.

## Important general notes

* This app does not use the Android development environment which
* should make working with it much easier.
* This app has no GUI.
* This app works by receiving guest science commands from the Ground
  Data System (GDS) and then executing these commands. GDS is the
  workbench for Astrobee.
* This app assumes Astrobee is located in the JPM module.
* Astrobee is started at point (10.10, -9.75, 4.20).
* How to use this demo inside the Gateway module instead of JPM is 
  described later on.

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

### Building the code

  cd $ANDROID_PATH/guest_science_projects/microphone-example
  ./gradlew build

### Start the Astrobee simulator

In one terminal, with the above environment variables set, run:

roslaunch astrobee sim.launch speed:=0.75 gds:=true rviz:=true \
  pose:="10.20 -9.00 5.40 0 0 0 1"

This will launch GDS to control Astrobee and RViz to see its
trajectory. We will use the Bumble robot, with the above pose.

Once GDS and Rviz loaded fully, in another terminal, with the
environment set up as above, run:

  cd $ANDROID_PATH/guest_science_projects/microphone-example
  ./gradlew run

### Running the Guest Science Manager

Execute this sequence of actions.

1. Go to the `Teleoperate` tab in GDS. Click on `Grab Control`.

2. Ensure that the `Manual Move Inputs` there agree with the bumble
   pose specified earlier. If it does not, one may click on `Snap
   Preview to Bee` or manually modify those inputs.

3. Go to the `Advanced Guest Science` tab.

4. Check the `Bumble` checkbox. On the same line, ensure that you can
   see and click on the `Microphone` application. If this does not
   work, something failed at previous steps.

5. In the APKs section, choose the `Microphone` app from the dropdown
   menu and click on `Start APK`.

6. In the `Manual Commanding` section, choose the `Microphone` app
   from the APK dropdown menu.

7. Under `Template`, pick one of the actions to run. It can be either
   performing the initial search for leak, or a subsequent refined
   search, or doing both of these at the same time.

8. In RViz, add a topic "By Display Type" of MarkerArray, and set
   its topic as 

  /gs/microphone

   (or see gateway.rviz in the freeflyer repository). One can save in
   RViz the configuration with this topic added to use it next time
   around.

9. Click on the `Send Command` button in GDS, and then visualize the
    results in RViz. It may take some time for Astrobee to move.
    Brighter colors along bot's trajectory in RViz means that the
    sound is louder at that location.

To echo the messages published by this app, do

  rostopic echo /gs/microphone

### Running this app on the Gateway

Assuming that the demo was run successfully, one can attempt to run it
inside the Gateway module. For that, one should do the following before
following the steps above:

1. Overwrite 

  freeflyer/description/media/astrobee_iss/meshes/jpm.dae
  
  with the Gateway CAD model.

2. Edit MicrophoneMain.java and set useGateway to true. Also set
   skipGSManager to true, since we will avoid GDS, as it does not
   support Gateway.

3. Start the simulator as

  roslaunch astrobee sim.launch speed:=0.75 gds:=true rviz:=true \
    pose:="8.5 -8.2 7.9 0 0 0 1"
  
  Note that we did not start GDS and changed the starting pose.

4. Switch to RViz. Open the config file
  
  freeflyer/astrobee/resources/rviz/gateway.rviz

  Ensure that the Microphone checkbox is checked.
 
5. Run as above the microphone example using gradlew and track its progress 
   in RViz.

