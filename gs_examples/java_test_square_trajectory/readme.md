# Simple Square Trajectory Example

This example is meant to be a quick introduction to the Astrobee API 
Development. This project has been wrote to be independent of the
API source. In order to do that we use jar libraries to import
the code for the Astrobee API and Freeflyer ROS messages.

*Note the APIs may change in time. Ensure to have the ultimate version of the
jar libraries.*

## Running the example

### Building the code

First, we build the example. Assuming that `freeflyer_android` is checked out 
in `$ANDROID_PATH`:

    you@machine:~ $ cd $ANDROID_PATH/gs_examples/java_test_square_trajectory
    you@machine:java_test_square_trajectory $ ./gradlew build

### Setup environmental variables

Assuming you have a proper build of the Astrobee flight software located in
`$BUILD_PATH`, the following will setup your environment to run the simulator
locally:

    you@machine:~ $ . $BUILD_PATH/devel/setup.bash
    you@machine:~ $ export ROS_IP=127.0.0.1
    you@machine:~ $ export ROS_MASTER_URI=http://${ROS_IP}:11311/

### Start the Astrobee simulator

In one terminal, with the environment variables setup:

    you@machine:~ $ roslaunch astrobee sim.launch dds:=false default:=false bumble:=true
    ...

### (Option 1) Run the example using Gradle

In another terminal, with the environment setup properly:

    you@machine:~ $ export ROS_NAMESPACE=bumble
    you@machine:~ $ cd $ANDROID_PATH/gs_examples/java_test_square_trajectory
    you@machine:java_test_square_trajectory $ ./gradlew run
    ...

### (Option 2) Run the example using Java VM

#### Generating jar file

In another terminal, with the environment setup properly:

    you@machine:~ $ cd $ANDROID_PATH/gs_examples/java_test_square_trajectory
    you@machine:java_test_square_trajectory $ ./gradlew jar
    ...

#### Run file

In the same terminal and assuming you have Java installed. Execute:

    you@machine:java_test_square_trajectory $ export ROS_NAMESPACE=bumble
    you@machine:java_test_square_trajectory $ java -jar build/libs/java_test_square_trajectory-1.0-SNAPSHOT.jar

## Importing into IntelliJ IDEA

 * Open IntelliJ and click `Import Project`.
 * Navigate to the root of the `java_test_square_trajectory` directory (this directory)
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

