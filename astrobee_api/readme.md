# Astrobee Robotics Software Guest Science Simple API

## Getting Started

### Generating ARS ROS Messages (`ff_msgs`)

Assuming you have a checkout of the freeflyer git repo located at
`$SOURCE_PATH` and you are building in `$BUILD_PATH`

Ensure `rosjava` is installed:

    you@machine:~ $ sudo apt-get install ros-kinetic-rosjava

Configure a native build like normal

Build the ff_msgs jar

    you@machine:~ $ cd $BUILD_PATH
    you@machine:native $ make rebuild_cache
    you@machine:native $ make ff_msgs_generate_messages_java_gradle

### Letting gradle access ARS messages

The astrobee_api project is configured to look at the local maven repository
to access the `ff_msgs` jar. The default location for this repository is:
`$HOME/.m2/repository`. If you are building this project on the same machine
that you build flight software on, the easiest thing to do is symlink the
rosjava generated files into the right location:

    you@machine:~ $ mkdir -p $HOME/.m2
    you@machine:.m2 $ cd $HOME/.m2
    you@machine:.m2 $ ln -s $BUILD_PATH/devel/share/maven repository

Otherwise you will have to copy the contents of the maven directory out
of `devel/shared` into `$HOME/.m2/repository`.

### Running the example

There is a simple example showing how to use the API included. It is a very
trivial example that simply moves the robot 0.5 meters in the +X direction.
You can find the source to the example in the `ros-example` directory.

It is possible to run the example from the commandline using gradle. First, we
build the example. Assuming that `astrobee_android` is checked out in
`$ANDROID_PATH`:

    you@machine:~ $ cd $ANDROID_PATH/astrobee_api
    you@machine:astrobee_api $ ./gradlew build

#### Setup environment variables

Assuming you have a proper build of the Astrobee flight software located in
`$BUILD_PATH`, the following will setup your environment to run the simulator
locally:

    you@machine:~ $ . $BUILD_PATH/devel/setup.bash
    you@machine:~ $ export ROS_IP=127.0.0.1
    you@machine:~ $ export ROS_MASTER_URI=http://${ROS_IP}:11311/

#### Start the Astrobee simulator

In one terminal, with the evironment variables setup:

    you@machine:~ $ roslaunch astrobee sim.launch dds:=false
    ...

#### Start `ros-example`

In another terminal, with the environment setup properly:

    export ROS_MASTER_URI=http://${ROS_IP}:11311/
    you@machine:~ $ cd $ANDROID_PATH/astrobee_api
    you@machine:astrobee_api $ ./gradlew run
    ...

### Importing into IntelliJ

 * Open IntelliJ and click `Import Project`.
 * Navigate to the root of the `astrobee_api` directory (this directory)
 * Select `Import project from external model`
 * Select `Gradle`
 * Click `Next`
 * Uncheck `Use auto-import`
 * Uncheck `Create directories...`
 * Check `Create separate module per source set`
 * Uncheck `Store generated project files externally`
 * Select `Use default gradle wrapper`
 * Select `.idea (Directory Based)` Project format
 * Click `Finish`

## Architecture

The API has been split into the core API layer and an implementation layer.

The `api` module defines an independent API which can be implemented for
whatever underlying system is needed, whether that be ROS or Android. An
implementation for ROS is located under the `ros` module.

The `api` module also consists of generated Java sources for the Robot and base
implementations of that interface from the command dictionary schema. These
files are located under the `src/main/generated` folder and should *not* be
modified.

A simple example based on ROS is available under the `ros-example` module. This
uses the `api` and `ros` modules.
