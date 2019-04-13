# Test Guest Science API

This is a Guest Science Android application that uses the Guest Science
library and the Astrobee API. It is an example of a primary apk that commands
the robot to execute trajectories and arm movements.

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

This example defines 9 commands:

 * Move To: Performs a simple move command to a specific point
   (inside the JEM module). Final orientation will be the default orientation
   (0, 0, 0, 1)
 * Move in X: Performs a move command relative to the Astrobee position in the
   X-axis. This command adds to the value already in place:
     currentPositionX + (add value).
 * Move in Y: Performs a relative move command in the Y axis.
 * Move in Z: Performs a relative move command in the Z axis.
 * Dock: Dock Astrobee if the robot meets the criteria to perform a dock
   operation.
 * Undock: Undock Astrobee if it is currently docked.
 * Get Robot Position: Returns the current robot position (XYZ).
 * Deploy Arm: Move perching arm to fully deployed position (TILT)
 * Stow Arm: Move perching arm to the stowed position (TILT)
