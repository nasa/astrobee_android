# Simple Square Trajectory Example

This a Guest Science Java only application. This example is meant to be a quick
introduction to the Astrobee API Development. This project has been written to
be independent of API source. In order to do so, we use jar libraries to import
the code for the Astrobee API, Freeflyer ROS messages and GS Stub. It is an
example of a primary apk that commands the robot to execute a simple trajectory.

* Note 1. These instructions assume you have read the Guest Science
  Documentation included in this repository.
* Note 2. Two terminals will be used along this procedure: we will refer to them
  as Terminal 1 (Astrobee simulation) and Terminal 2 (Java application)

## Running the example

### Setup environmental variables

In a terminal (Terminal 1) and assuming you have a proper build of the Astrobee
flight software located in `$BUILD_PATH`, the following will setup your
environment to run the simulator locally:

```shell
    # This may be $HOME/freeflyer_build/native
    you@machine:~ $ export BUILD_PATH="Type here your build_path"
    you@machine:~ $ export ROS_IP=127.0.0.1
    you@machine:~ $ export ROS_MASTER_URI=http://${ROS_IP}:11311/
    you@machine:~ $ . $BUILD_PATH/devel/setup.bash
```

### Start the Astrobee simulator

In Terminal 1, with the environment variables setup, execute one of the
following:

**NASA Users**
```shell
    you@machine:~ $ roslaunch astrobee sim.launch gds:=true
```

**Non-NASA Users**
```shell
    you@machine:~ $ roslaunch astrobee sim.launch rviz:=true dds:=false
```

### Building the code

In a new terminal (Terminal 2) we build the example. Assuming that
`freeflyer_android` is checked out in `$ANDROID_PATH`:

```shell
    # If you are using a standalone repo, the path probably will be:
    #     $HOME/freeflyer_android
    # If you are using this repo as a submodule, the path may be:
    #     $HOME/freeflyer/submodules/android
    you@machine:~ $ export ANDROID_PATH="Type here the location of Freeflyer Android"
    you@machine:~ $ cd $ANDROID_PATH/gs_examples/java_test_square_trajectory
    you@machine:java_test_square_trajectory $ ./gradlew build
```

### (Option 1) Run the example using Gradle

In Terminal 2, with the environment setup properly:

```shell
    you@machine:java_test_square_trajectory $ ./gradlew run
```

### (Option 2) Run the example using Java VM

#### Generating jar file

In Terminal 2, with the environment setup properly:

```shell
    you@machine:java_test_square_trajectory $ ./gradlew jar
```

#### Run file

In Terminal 2 and assuming you have Java installed. Execute:

```shell
    you@machine:java_test_square_trajectory $ java -jar build/libs/java_test_square_trajectory-1.0-SNAPSHOT.jar
```

### Commanding example using Guest science

At this moment, Java Test Square Trajectory is running and waiting for commands.
Please refer to [Running GS Applications](../../running_gs_app.md) to learn how
to command this example.

## (Additional information) Importing into IntelliJ IDEA

Please note this may be outdated.

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
