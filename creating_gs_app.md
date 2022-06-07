# Creating a Guest Science Application

This readme assumes you have followed the
[Guest Science Developer Guide](gs_developer_guide.md).

It also assumes that `freeflyer_android` is checked out in `$ANDROID_PATH`,
`freeflyer` is checked out in `$SOURCE_PATH`, and you are building in
`$BUILD_PATH`.

Astrobee has two major libraries for Guest Science developers: the Astrobee API
and the Guest Science Library. It is important for a Guest Scientist to
understand what these libraries do. Please see the About sections in the
[astrobee api readme](astrobee_api/README.md#about) and
[guest science readme](guest_science/README.md#about) documentation for
understanding.

This readme teaches you how to import our libraries into a new or existing
Guest Science project. All developers should follow the [setup](#setup) and
[build the Astrobee API](#build-the-astrobee-api) sections. After these
sections, the Java only developers should follow the [Java only](#java-only)
section and the emulator and HLP board developers should follow the
[Android](#Android) section.

After importing our libraries, please look at our [examples](gs_examples/README.md) for guidance on how
to develop a Guest Science Application, and use the API functions.

If you are creating a new Guest Science Application and are looking for a place
to put it, we have created a `guest_science_projects` folder for you to use. 
Please note, you don't have to use this directory and can store your project
anywhere on your computer.

## Setup

In every terminal you use, be sure to set up your environment. If you forgot how
to do this, follow the Setting up your Environment section of the
[`freeflyer/simulation/sim_overview.md`](https://github.com/nasa/astrobee/blob/master/simulation/sim_overview.md#setting-up-your-environment).

## Build the Astrobee API

### Generating ARS ROS Messages (`ff_msgs`)

Ensure `rosjava` is installed:

    you@machine:~ $ sudo apt-get install ros-kinetic-rosjava

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

### Building the JAR Files

    you@machine:~ $ cd $ANDROID_PATH/astrobee_api
    you@machine:astrobee_api $ ./gradlew build

## Java Only

Coming soon!!!

<!--- Make sure to mention that java only applications will not run in SPAAAACE!-->

<!--- Don't forget to have them include the gs-stub library -->

### Converting the Java Only Code to an APK

Coming soon!!!

## Android

Please refer to Android Studio documentation if you need help creating a new
Android project. When you setup your project, make sure to use API 25
(Nougat 7.1.1).

### Building the Guest Science Library

!!! DISCLAIMER !!!
We are still working on a way to import the GS library without having to do the 
following. Sorry for any inconvenience.

 * Open Android Studio and click `Open an existing Android Studio project`
 * Navigate to the root dictory of the `guest_science` project (this directory)
 * Click `OK`
 * In the menu bar, click `Build` and select `Build APK`

### Renaming the Guest Science Library

The GS library created in the last section has a generic name. In order to avoid
conflict with other custom libraries, please rename it.

    you@machine:~$ cd $ANDROID_PATH/guest_science/library/build/outputs/aar
    you@machine:guest_science/library/build/outputs/aar$ mv library-debug.aar guest_science_library.aar

### Importing the Astrobee API

You will be using the command line to import the Astrobee API. Please set 
`PROJECT_ROOT` to be the root folder of your project.

    you@machine:~$ cd $PROJECT_ROOT
    you@machine:~$ cp $ANDROID_PATH/astrobee_api/api/build/libs/api-1.0-SNAPSHOT.jar app/libs
    you@machine:~$ cp $ANDROID_PATH/astrobee_api/ros/build/libs/ros-1.0-SNAPSHOT.jar app/libs
    you@machine:~$ cp $BUILD_PATH/devel/share/maven/org/ros/rosjava_messages/ff_msgs/0.0.0/ff_msgs-0.0.0.jar app/libs

**Note** If the app/libs folder doesn't exist, you will have to create it.

### Adding the Guest Science Library to Your Android Studio Project

Please open your project in Android Studio and then do the following:

 * In the menu bar, click `File` > `New` > `New Module`
 * Select `Import .JAR/.AAR Package` and click `Next`
 * Navigate to $ANDROID_PATH/guest_science/library/build/outputs/aar
 * Select the `guest_science_library.aar` and click `OK`
 * Click `Finish`
 * Navigate to the 'Project' section of the Android Studio window
 * If the `Gradle Scripts` portion is not expanded, click on the sideway triangle to the left of `Gradle Scripts`
 * Double click on `build.gradle (Module: app)`
 * In the dependencies section, please add the line `compile project(":guest_science_library")'

### Telling Gradle about ROS and the Astrobee API

Please open the project (top level) `build.gradle` file with either Android
Studio or the text editor of your choose. Edit your `allprojects` section to 
include maven. It should look similar to this:

  ```shell
    allprojects {
        repositories {
            maven {
                url "https://github.com/rosjava/rosjava_mvn_repo/raw/master"
            }
            jcenter()
        }
    }
  ```

Please note, you will need an internet connection so that Androind studio can
pull the rosjava libraries it needs.

Next open your app module build.gradle. In the dependencies section, check that
the following line exists:

  ```shell
    compile fileTree(dir: 'libs', include: ['*.jar'])
  ```

If it doesn't, please add it.

#### Start Service

The GS library contains a class called `StartGuestScienceService`. This class is
a Android service that will run in the background and takes care of the
communication between the GS manager and the GS apk. You will need to extend
this class and override some of the functions.

The GS manager needs to know about the service you created that extended the
start GS service class. Thus when you add your service to the Android manifest
file, you will also need to add meta data specifying that this is the service
you want started when the GS manager goes to start your apk. Please see below
for an example of how the service should look in the manifest.

  ```shell
    <service android:name=".StartTestGuestScienceApkService" android:exported="true">
      <meta-data android:name="Start Service" android:value="true" />
    </service>
  ```

**Now you are ready to develop your Guest Science Application. If you need more
guidance, please see our [examples](gs_examples/README.md) for guidance on how
to develop a Guest Science Application, and use the API functions.**
 
