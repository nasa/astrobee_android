// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.internal;

import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.types.Mat33f;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.types.Vec3d;
import gov.nasa.arc.astrobee.types.ActionType;
import gov.nasa.arc.astrobee.types.CameraMode;
import gov.nasa.arc.astrobee.types.CameraName;
import gov.nasa.arc.astrobee.types.CameraResolution;
import gov.nasa.arc.astrobee.types.FlashlightLocation;
import gov.nasa.arc.astrobee.types.FlightMode;
import gov.nasa.arc.astrobee.types.LocalizationMode;
import gov.nasa.arc.astrobee.types.PlannerType;
import gov.nasa.arc.astrobee.types.PoweredComponent;
import gov.nasa.arc.astrobee.types.TelemetryType;

public abstract class BaseRobotImpl extends AbstractRobot implements BaseRobot {

    @Override
    public PendingResult grabControl(String cookie) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("grabControl")
                .addArgument("cookie", cookie);
        return publish(builder.build());
    }

    @Override
    public PendingResult requestControl() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("requestControl");
        return publish(builder.build());
    }

    @Override
    public PendingResult fault() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("fault");
        return publish(builder.build());
    }

    @Override
    public PendingResult initializeBias() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("initializeBias");
        return publish(builder.build());
    }

    @Override
    public PendingResult loadNodelet(String nodeletName,
                                     String managerName,
                                     String type,
                                     String bondId) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("loadNodelet")
                .addArgument("nodeletName", nodeletName)
                .addArgument("managerName", managerName)
                .addArgument("type", type)
                .addArgument("bondId", bondId);
        return publish(builder.build());
    }

    @Override
    public PendingResult noOp() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("noOp");
        return publish(builder.build());
    }

    @Override
    public PendingResult reacquirePosition() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("reacquirePosition");
        return publish(builder.build());
    }

    @Override
    public PendingResult resetEkf() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("resetEkf");
        return publish(builder.build());
    }

    @Override
    public PendingResult switchLocalization(LocalizationMode mode) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("switchLocalization")
                .addArgument("mode", mode);
        return publish(builder.build());
    }

    @Override
    public PendingResult unloadNodelet(String nodeletName,
                                       String managerName) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("unloadNodelet")
                .addArgument("nodeletName", nodeletName)
                .addArgument("managerName", managerName);
        return publish(builder.build());
    }

    @Override
    public PendingResult unterminate() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("unterminate");
        return publish(builder.build());
    }

    @Override
    public PendingResult wake(int berthNumber) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("wake")
                .addArgument("berthNumber", berthNumber);
        return publish(builder.build());
    }

    @Override
    public PendingResult wakeSafe(int berthNumber) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("wakeSafe")
                .addArgument("berthNumber", berthNumber);
        return publish(builder.build());
    }

    @Override
    public PendingResult armPanAndTilt(float pan,
                                       float tilt,
                                       ActionType which) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("armPanAndTilt")
                .addArgument("pan", pan)
                .addArgument("tilt", tilt)
                .addArgument("which", which);
        return publish(builder.build());
    }

    @Override
    public PendingResult deployArm() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("deployArm");
        return publish(builder.build());
    }

    @Override
    public PendingResult gripperControl(boolean open) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("gripperControl")
                .addArgument("open", open);
        return publish(builder.build());
    }

    @Override
    public PendingResult stopArm() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("stopArm");
        return publish(builder.build());
    }

    @Override
    public PendingResult stowArm() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("stowArm");
        return publish(builder.build());
    }

    @Override
    public PendingResult setDataToDisk() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setDataToDisk");
        return publish(builder.build());
    }

    @Override
    public PendingResult startRecording(String description) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("startRecording")
                .addArgument("description", description);
        return publish(builder.build());
    }

    @Override
    public PendingResult stopRecording() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("stopRecording");
        return publish(builder.build());
    }

    @Override
    public PendingResult customGuestScience(String apkName, String command) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("customGuestScience")
                .addArgument("apkName", apkName)
                .addArgument("command", command);
        return publish(builder.build());
    }

    @Override
    public PendingResult restartGuestScience(String apkName, int wait) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("restartGuestScience")
                .addArgument("apkName", apkName)
                .addArgument("wait", wait);
        return publish(builder.build());
    }

    @Override
    public PendingResult startGuestScience(String apkName) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("startGuestScience")
                .addArgument("apkName", apkName);
        return publish(builder.build());
    }

    @Override
    public PendingResult stopGuestScience(String apkName) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("stopGuestScience")
                .addArgument("apkName", apkName);
        return publish(builder.build());
    }

    @Override
    public PendingResult autoReturn(int berthNumber) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("autoReturn")
                .addArgument("berthNumber", berthNumber);
        return publish(builder.build());
    }

    @Override
    public PendingResult dock(int berthNumber) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("dock")
                .addArgument("berthNumber", berthNumber);
        return publish(builder.build());
    }

    @Override
    public PendingResult idlePropulsion() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("idlePropulsion");
        return publish(builder.build());
    }

    @Override
    public PendingResult perch() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("perch");
        return publish(builder.build());
    }

    @Override
    public PendingResult prepare() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("prepare");
        return publish(builder.build());
    }

    @Override
    public PendingResult simpleMove6DOF(String referenceFrame,
                                        Point xyz,
                                        Vec3d xyzTolerance,
                                        Quaternion rot) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("simpleMove6DOF")
                .addArgument("referenceFrame", referenceFrame)
                .addArgument("xyz", xyz)
                .addArgument("xyzTolerance", xyzTolerance)
                .addArgument("rot", rot);
        return publish(builder.build());
    }

    @Override
    public PendingResult stopAllMotion() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("stopAllMotion");
        return publish(builder.build());
    }

    @Override
    public PendingResult undock() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("undock");
        return publish(builder.build());
    }

    @Override
    public PendingResult unperch() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("unperch");
        return publish(builder.build());
    }

    @Override
    public PendingResult pausePlan() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("pausePlan");
        return publish(builder.build());
    }

    @Override
    public PendingResult runPlan() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("runPlan");
        return publish(builder.build());
    }

    @Override
    public PendingResult setPlan() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setPlan");
        return publish(builder.build());
    }

    @Override
    public PendingResult skipPlanStep() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("skipPlanStep");
        return publish(builder.build());
    }

    @Override
    public PendingResult wait(float duration) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("wait")
                .addArgument("duration", duration);
        return publish(builder.build());
    }

    @Override
    public PendingResult powerOffItem(PoweredComponent which) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("powerOffItem")
                .addArgument("which", which);
        return publish(builder.build());
    }

    @Override
    public PendingResult powerOnItem(PoweredComponent which) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("powerOnItem")
                .addArgument("which", which);
        return publish(builder.build());
    }

    @Override
    public PendingResult enableAstrobeeIntercomms() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("enableAstrobeeIntercomms");
        return publish(builder.build());
    }

    @Override
    public PendingResult setCamera(CameraName cameraName,
                                   CameraMode cameraMode,
                                   CameraResolution resolution,
                                   float frameRate,
                                   float bandwidth) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setCamera")
                .addArgument("cameraName", cameraName)
                .addArgument("cameraMode", cameraMode)
                .addArgument("resolution", resolution)
                .addArgument("frameRate", frameRate)
                .addArgument("bandwidth", bandwidth);
        return publish(builder.build());
    }

    @Override
    public PendingResult setCameraRecording(CameraName cameraName,
                                            boolean record) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setCameraRecording")
                .addArgument("cameraName", cameraName)
                .addArgument("record", record);
        return publish(builder.build());
    }

    @Override
    public PendingResult setCameraStreaming(CameraName cameraName,
                                            boolean stream) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setCameraStreaming")
                .addArgument("cameraName", cameraName)
                .addArgument("stream", stream);
        return publish(builder.build());
    }

    @Override
    public PendingResult setCheckObstacles(boolean checkObstacles) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setCheckObstacles")
                .addArgument("checkObstacles", checkObstacles);
        return publish(builder.build());
    }

    @Override
    public PendingResult setCheckZones(boolean checkZones) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setCheckZones")
                .addArgument("checkZones", checkZones);
        return publish(builder.build());
    }

    @Override
    public PendingResult setEnableAutoReturn(boolean enableAutoReturn) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setEnableAutoReturn")
                .addArgument("enableAutoReturn", enableAutoReturn);
        return publish(builder.build());
    }

    @Override
    public PendingResult setEnableImmediate(boolean enableImmediate) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setEnableImmediate")
                .addArgument("enableImmediate", enableImmediate);
        return publish(builder.build());
    }

    @Override
    public PendingResult setEnableReplan(boolean enableReplan) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setEnableReplan")
                .addArgument("enableReplan", enableReplan);
        return publish(builder.build());
    }

    @Override
    public PendingResult setExposure(CameraName cameraName, float exposure) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setExposure")
                .addArgument("cameraName", cameraName)
                .addArgument("exposure", exposure);
        return publish(builder.build());
    }

    @Override
    public PendingResult setFlashlightBrightness(FlashlightLocation which,
                                                 float brightness) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setFlashlightBrightness")
                .addArgument("which", which)
                .addArgument("brightness", brightness);
        return publish(builder.build());
    }

    @Override
    public PendingResult setHolonomicMode(boolean enableHolonomic) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setHolonomicMode")
                .addArgument("enableHolonomic", enableHolonomic);
        return publish(builder.build());
    }

    @Override
    public PendingResult setInertia(String name,
                                    float mass,
                                    Vec3d centerOfMass,
                                    Mat33f matrix) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setInertia")
                .addArgument("name", name)
                .addArgument("mass", mass)
                .addArgument("centerOfMass", centerOfMass)
                .addArgument("matrix", matrix);
        return publish(builder.build());
    }

    @Override
    public PendingResult setMap(String mapName) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setMap")
                .addArgument("mapName", mapName);
        return publish(builder.build());
    }

    @Override
    public PendingResult setOperatingLimits(String profileName,
                                            FlightMode flightMode,
                                            float targetLinearVelocity,
                                            float targetLinearAcceleration,
                                            float targetAngularVelocity,
                                            float targetAngularAcceleration,
                                            float collisionDistance) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setOperatingLimits")
                .addArgument("profileName", profileName)
                .addArgument("flightMode", flightMode)
                .addArgument("targetLinearVelocity", targetLinearVelocity)
                .addArgument("targetLinearAcceleration", targetLinearAcceleration)
                .addArgument("targetAngularVelocity", targetAngularVelocity)
                .addArgument("targetAngularAcceleration", targetAngularAcceleration)
                .addArgument("collisionDistance", collisionDistance);
        return publish(builder.build());
    }

    @Override
    public PendingResult setPlanner(PlannerType planner) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setPlanner")
                .addArgument("planner", planner);
        return publish(builder.build());
    }

    @Override
    public PendingResult setTelemetryRate(TelemetryType telemetryName,
                                          float rate) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setTelemetryRate")
                .addArgument("telemetryName", telemetryName)
                .addArgument("rate", rate);
        return publish(builder.build());
    }

    @Override
    public PendingResult setZones() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setZones");
        return publish(builder.build());
    }

}
