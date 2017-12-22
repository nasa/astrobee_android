# Astrobee Robotics Software Guest Science Library

## Information
Guest science (GS) apks are started and stopped from ground commands. They can also receive custom commands from the ground. In order for all this to operate smoothly, there is a GS manager to manage the commands and states of the GS apks. Thus each GS apk needs to provide an Android framework for the GS manager to use. This framework has been established in the guest science library. Please read this documentation for information on how to use the GS library. If you can't use the guest science library or want more control, please see [advanced usage instructions.](advanced.md) 

This readme mainly focuses on how to use the guest science library. If you would like more information on GS experiments or GS apk interaction with our Ground Data System, please see IRG-FF029-Astrobee-Guest-Science-Guide.pdf in freeflyer_docs/GuestScience and GuestScienceNotes.pdf in freeflyer_docs/GuestScience/GuestScienceNotesDoc. Please note these documents are not part of the flight software repository so you may have to email your Astrobee contact or the flight software team if you don't have access to babelfish.

## Getting Started

Please follow the next three sections to get the GS library imported into your Android Studio project.

!!! DISCLAIMER !!!
We are still working on a way to import the GS library without having to do the next 3 Sections. Sorry for any inconvenience.

### Building the Guest Science Library

 * Open Android Studio and click `Open an existing Android Studio project`
 * Navigate to the root of the `guest_science` directory (this directory)
 * Click `OK`
 * In the menu bar, click `Build` and select `Build APK`

### Renaming the Guest Science Library

The GS library created in the last section has a generic name. In order to avoid conflict with other custom libraries, please rename it. In a terminal with the environment setup properly do the following:

    you@machine:~$ cd $ANDROID_PATH/guest_science/library/build/outputs/aar
    you@machine:guest_science/library/build/outputs/aar$ mv library-debug.aar guest_science_library.aar

### Adding the Guest Science Library to Your Android Studio Project

Please open your project your in Android Studio and then do the following:

 * In the menu bar, click `File` > `New` > `New Module`
 * Select `Import .JAR/.AAR Package` and click `Next`
 * Navigate to $ANDROID_PATH/guest_science/library/build/outputs/aar
 * Select the `guest_science_library.aar` and click `OK`
 * Click `Finish`
 * Navigate to the 'Project' section of the Android Studio window
 * If the `Gradle Scripts` portion is not expanded, click on the sideway triangle to the left of `Gradle Scripts`
 * Double click on `build.gradle (Module: app)`
 * In the dependencies section, please add the line `compile project(":guest_science_library")'

### How to Use the Guest Science Library

The GS library should be relatively easy to use. The next few sections will explain how the GS library works and what you need to do to use it successfully. For a better understanding of how a GS apk should work with the GS library, please see the examples section.

#### APK Information and Custom Commands

The GS manager sends a configuration message to the ground that populates the Ground Data System (GDS). This configuration message lets the ground operators and/or crew know which APKs are installed on HLP, if they are primary or secondary, and the custom commands they accept. The GS library provides an easy way for a GS apk to get this information to the GS manager. The GS library extracts this information from a commands.xml file that needs to be created in the res/xml folder. This folder is not created by default. To create it, please do the following:

 * Navigate to the `Project` section of the Android Studio window
 * Right click on the `res` directory
 * Click `New` > `Android resource directory`
 * For the `Directory name`, enter xml
 * Change the `Resource type` to xml
 * Click `OK`

To create the commands.xml file, please do the following:

 * Navigate to the `Project` section of the Android Studio window
 * If the `res` directory isn't expanded, click on the sideways triangle to the left of `res`
 * Right click `xml`
 * Click `New` > `XML resource file`
 * For the `File name`, enter commands.xml
 * Click `OK`
 * Android Studio will open up the file in there `Design` window. Please click the `Text` tab at the bottom of the file window

The only thing required in the commands.xml file is whether the apk is primary or secondary. If the GS apk has custom commands, these should also be added. The full name and short name of the apk are extracted automatically but if you would like a different short name, please put it in this file. See below for an example of how the commands.xml file should be formatted.

    <?xml version="1.0" encoding="utf-8"?>
    <apkInfo>
      <shortName>test_apk</shortName> <!-- Optional -->
      <primary>true</primary> <!-- Required -->
      <commands>
        <command
          name="No Op"
          syntax="{&quot;name&quot;: &quot;noOp&quot;}" />
        <command
          name="Do Something"
          syntax="{&quot;name&quot;: &quot;doSomething&quot;, &quot;numOfTimes&quot;: &quot;5&qout;}" />
      </commands>
    </apkInfo>

#### Start Service

The GS library contains a class called `StartGuestScienceService`. This class is a Android service that will run in the background and takes care of the communication between the GS manager and the GS apk. You will need to extend this class and override some of the functions. Please the next section for more information on the functions you need to override.

The GS manager needs to know about the service you created that extended the start GS service class. Thus when you add your service to the Android manifest file, you will also need to add meta data specifying that this is the service you want started when the GS manager goes to start your apk. Please see below for an example of how the service should look in the manifest.

    <service android:name=".StartTestGuestScienceApkService" android:exported="true">
      <meta-data android:name="Start Service" android:value="true" />
    </service>

##### Functions to Override

onGuestScienceStart - This function is called when the GS manager starts your apk. Put all of your start up code in here.

onGuestScienceCustomCmd - This function is called when the GS manager sends a custom command to your apk. Please handle your commands in this function.

onGuestScienceStop - This function is called when the GS manager stops your apk. Put all of your clean up code in here. You should also call the terminate helper functioni at the very end of this function.

##### Helper Functions

sendStarted - GDS will display certain types of GS data. The data it will display must be in JSON format and it will only display the value paired with name "Summary". This function will send a JSON string with a name/value pair of "Summary" and "Started". This will let a ground controller and/or crew member know that your apk was started successfully. This command should be called at the end of the `onGuestScienceStart` function. Parameters:

 * String topic - Can only be 32 characters long. Topic is sent down in the GS data message but not currently used for anything. You can give the function an empty string if you have no use for it. If you plan on doing something with the GS data messages on the ground, you may want to have topics like information, data, etc. 

sendStopped - This function will send a JSON string with a name/value pair of "Summary" and "Stopped". This will let a ground controller and/or crew member know that your apk stopped successfully. This command should be called in the `onGuestScienceStop` function but before the terminate function is called. Parameters:

 * String topic - Can only be 32 characters long. Topic is sent down in the GS data message but not currently used for anything. You can give the function an empty string if you have no use for it. If you plan on doing something with the GS data messages on the ground, you may want to have topics like information, data, etc.

sendReceivedCustomCommand - This function will send a JSON string with a name/value pair of "Summary" and "Received Custom Command". This command will let a ground controller and/or crew member know that your apk received the custom command. Parameters: 

 * String topic - Can only be 32 characters long. Topic is sent down in the GS data message but currently not used for anything. You can give the function an empty string if you have no use for it. If you plan on doing something with the GS data messages on the ground, you may want to have topics like information, data, etc.

sendData - This function will send any data you give it to the ground. If it isn't formatted as a JSON string and doesn't have a name/value pair with a name of "Summary", the data(value) will not be displayed in GDS. If you are planning on  doing something with the GS data message on the ground, feel free to send anything you want. Parameters:

 * MessageType type - Type of data you are sending. This lets the ground know what kind of data is in the message. The choices are JSON, STRING, and BINARY.
 * String topic - Can only be 32 characters long. Topic is sent down in the GS data message but currently not used for anything. You can give the function an empty string if you have no use for it. If you plan on doing something with the GS data messages on the ground, you may want to have topics like information, data, etc.
 * String or byte[] data - Must not exceed 2048 bytes. The data the gs apk wants to send to the ground.

terminate - This function kills the process this service is running in. This function should be called at the very end of the `onGuestScienceStop`. It takes no parameters.

#### Examples

There are three example apks that use the guest science library. See below for a brief description of each apk. It can be hard to explain how to use a library in a document so if you have any questions, please open and look at any of the examples. They may be able to clarify anything unclearly documented above. The AndroidManifest.xml, Start\*\*\*Service.java, and commands.xml files will be most helpful.

Please note that these only make use of the gs manager but the hope is that they will eventually use the astrobee api to provide a more complete guest science apk example.

test_guest_science_apk - Is a primary apk that has a GUI\*. It will simulate some sort of image sampler.

test_rfid_reader - Is a primary apk that will simulate some sort of rfid reader.

test_air_sampler - Is a secondary apk that will simulate some sort of air sampler.

* While GUIs are useful in testing, most GS apks shouldn't have a GUI since a crew member will most likely not be looking at the GUI.
