# Astrobee Robot Software - Android submodule

## About

Astrobee is a free-flying robot that is designed to operate as a payload inside
the International Space Station (ISS). The Astrobee Robot Software consists of
embedded (on-board) software, supporting tools and a simulator. The Astrobee
Robot Software operates on Astrobee's three internal single board computers and
uses the open-source Robot Operating System (ROS) framework as message-passing
middleware. The Astrobee Robot Software performs vision-based localization,
provides autonomous navigation, docking and perching, manages various sensors
and actuators, and supports user interaction via screen-based displays, light
signaling, and sound. The Astrobee Robot Software enables Astrobee to be
operated in multiple modes: plan-based task execution (command sequencing),
teleoperation, or autonomously through execution of hosted code uploaded by
project partners (guest science). The software simulator enables Astrobee Robot
Software to be evaluated without the need for robot hardware.

This repository contains the libraries and API to support Guest Science
application running on the Astrobee High Level Processor (HLP). The HLP runs the
[Android Nougat](https://www.android.com/versions/nougat-7-0/)
Operating System (7.1.1). The Astrobee Robot Software exposes a Java
API that can be used either in pure Java land or Android land to interact with
robot internal messaging system based on ROS.

A distinct repository, [`astrobee`](https://github.com/nasa/astrobee), contains
the core flight software for Astrobee. Note that `astrobee` repository is
required to used `astrobee_android` (it contains all the message definitions).

Also note that the Astrobee Robot Software is in beta stage. This means that
some features are missing and some functionalities are incomplete. Please
consult [RELEASE.md](https://github.com/nasa/astrobee/blob/HEAD/RELEASE.md) for
the current list of features and limitations.

Please see the [Guest Science Readme](guest_science_readme.md) for a description
of how guest science should interact with the Astrobee flight software. This
documentation also contains examples to help guest science interface with
Astrobee.

Please see the
[Guest Science Resources](https://www.nasa.gov/content/guest-science-resources)
page for information on guest science capabilities on Astrobee.

## Prerequisites

Before building the Astrobee Android project, ensure your system meets the following requirements:

- **Operating System**: Ubuntu 16.04 (Xenial Xerus) is needed for compatibility with `ros-kinetic-rosjava` dependencies. 
    - Ubuntu 16.04 support will soon be phased out as we migrate to Ubuntu 20.04
    - `rosjava` has not yet been released for ROS Noetic on Ubuntu 20.04, so `ros-kinetic-rosjava` needs to be built from source. [ROS Wiki Source Installation Instructions](http://wiki.ros.org/rosjava/Tutorials/kinetic/Source%20Installation).
        - Currently there are [dependency conflicts](https://github.com/nasa/astrobee_android/issues/44) when using `ros-kinetic-rosjava` with ROS Noetic on Ubuntu 20.04.
- **Android Studio**: Use version 3.6.3, which is compatible with the project's Gradle 3.3. Download from the [Android Studio archive](https://developer.android.com/studio/archive).
- **Java JDK**: Install Java JDK 8 for compatibility with the project's Gradle version. Download from the [Oracle website](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html).

Ensure these tools and dependencies are installed and configured before attempting to build the Astrobee Android project.

## License

Copyright (c) 2017, United States Government, as represented by the
Administrator of the National Aeronautics and Space Administration.
All rights reserved.

The Astrobee platform is licensed under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance with the License. You
may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
