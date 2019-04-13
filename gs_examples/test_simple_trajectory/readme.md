# Test Simple Trajectory

This is a Guest Science Android application that uses the Guest Science
Library and the Astrobee API. It is an example of a primary APK that commands
the robot to execute a simple trajectory.

* Note 1. These instructions assume you have read the Guest Science
  Documentation included in this repository.

## Important general notes

* This app has no GUI.
* This app assumes Astrobee is docked in the JEM module
* This app assumes you have a proper setup environment for Astrobee.
  For further information, please refer to the
  [Guest Science Developer Guide](../../gs_developer_guide.md).

## Execute the example

Please refer to [Running Guest Science Applications](../../running_gs_app.md)
to learn how to use this example.

### Available commands

This example defines 2 simple commands:

 * Run trajectory: Undocks Astrobee and execute a predefined 4 point trajectory
   inside the JEM module. It will return a DONE message if successful, or an 
   error description if not.
 * Get GS Path: Get the path to the folder created for this specific example.


 
