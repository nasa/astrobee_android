# Test Battery Monitor

This is a guest science android application that uses the guest science 
library and simple ROS communications to get data from the batteries. It is 
also an example of a secondary apk that has a GUI. While GUIs are useful in 
testing, most GS apks shouldn't have a GUI since a crew member will most 
likely not be looking at it.

## Important general notes

* This app works be receiving commands from the Ground Data System.
* The Ground Data System is the workbench for Astrobee. Contact the Astrobee 
  team for further information.
* This app assumes the Astrobee batteries are present or being simulated.
* This app assumes the device/board/emulator has set 10.42.0.33 as its IP address.
* This app assumes the ROS_IP is set to _10.42.0.1_ and the ROS_MASTER_URI to
  _http://10.42.0.1:11311_

## Execute the example

### Prerequisites

* You must have the latest version of the Astrobee Workbench 
  (Ground Data System). Assuming your Astrobee flight software is located in
  `$HOME`, you must unzip the workbench files in _$HOME/gds/latest_
* You must have this android app already installed in your device/board/emulator 
  and the network set up properly.
* You must have the Guest Science Manager app already installed in your
  device/board/emulator. See the guest_science_manager project at the root of
  this repository for further information.

### Setup environment variables

Assuming you have a proper build of the Astrobee flight software located in
`$BUILD_PATH`, the following will setup your environment to run the simulator
locally:

    you@machine:~ $ . $BUILD_PATH/devel/setup.bash
    you@machine:~ $ export ROS_IP=10.42.0.1
    you@machine:~ $ export ROS_MASTER_URI=http://${ROS_IP}:11311/

### Start the Astrobee simulator

In one terminal, with the environment variables setup:

    you@machine:~ $ roslaunch astrobee sim.launch gds:=true pose:="1 0 4.8 0 0 0 1"
    ...

This command will launch the Astrobee simulator, the Ground Data System and 
finally, it will set Astrobee in the given pose (first 3 numbers for position 
and the next 4 for orientation).

### Start the EPS simulator (Astrobee electrical supply)

In another terminal, with the environment variables setup:

    you@machine:~ $ roslaunch eps_sim eps_sim.launch

This command will launch software to simulate power levels in the batteries.

### Running the Guest Science Manager

Ensure your android device is running the Guest Science Manager. You can start 
it manually by starting the app named this way and pushing the START button.

### Running the application

Go to the graphical interface launched by the simulator (Ground Data System).
Then execute this sequence of actions:

1. Go to the Advanced Guest Science tab.
2. Check the Bumble checkbox.
3. Ensure you see the example app listed in the APKs dropdown menu in this section.
4. Click grab control.
5. In the APKs section, choose the example app from the dropdown menu and click Start.
6. In the Manual Commanding section, choose the example app from the APK dropdown menu.
7. In the same section, choose the `Get ALL Batteries Data` option from the 
   template dropdown. There are other command you may want to try.
8. Click `Send Command` button and watch the data you get in return.

If you can't see the app listed in the dropdown menu, that means there is no 
communication between the Ground Data System and the Guest Science Manager, 
please verify your network settings.

### Adding batteries to the simulation

Assuming you have a proper source of the Astrobee flight software located in
`$SOURCE_PATH`, the following script may be used to spawn and delete 
batteries from the EPS simulation:

    $SOURCE_PATH/scripts/debug/add_battery.sh

    USAGE: add_battery.sh <BATTERY_LOCATION> <ENABLED>

    BATTERY_LOCATION: tl (top left), tr (top right), bl (bottom left), 
    br (bottom right).

    ENABLED: true, false

For instance:

    you@machine:~ $ $SOURCE_PATH/scripts/debug/add_battery.sh tl true


