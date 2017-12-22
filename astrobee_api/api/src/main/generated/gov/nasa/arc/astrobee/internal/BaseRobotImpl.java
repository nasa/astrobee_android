
/* Copyright (c) 2017, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 *
 * All rights reserved.
 *
 * The Astrobee platform is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package gov.nasa.arc.astrobee.internal;

import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.types.Mat33f;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.types.Vec3d;
import gov.nasa.arc.astrobee.types.ActionType;
import gov.nasa.arc.astrobee.types.CameraName;
import gov.nasa.arc.astrobee.types.CameraResolution;
import gov.nasa.arc.astrobee.types.DownloadMethod;
import gov.nasa.arc.astrobee.types.FlashlightLocation;
import gov.nasa.arc.astrobee.types.FlightMode;
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
    public PendingResult loadNodelet(String nodeletName,
                                     String type,
                                     String managerName,
                                     String bondId) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("loadNodelet")
                .addArgument("nodeletName", nodeletName)
                .addArgument("type", type)
                .addArgument("managerName", managerName)
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
    public PendingResult shutdown() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("shutdown");
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
    public PendingResult wake(int berthNumber) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("wake")
                .addArgument("berthNumber", berthNumber);
        return publish(builder.build());
    }

    @Override
    public PendingResult wipeHlp() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("wipeHlp");
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
    public PendingResult clearData(DownloadMethod dataMethod) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("clearData")
                .addArgument("dataMethod", dataMethod);
        return publish(builder.build());
    }

    @Override
    public PendingResult downloadData(DownloadMethod dataMethod) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("downloadData")
                .addArgument("dataMethod", dataMethod);
        return publish(builder.build());
    }

    @Override
    public PendingResult setDataToDisk() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setDataToDisk");
        return publish(builder.build());
    }

    @Override
    public PendingResult stopDownload(DownloadMethod dataMethod) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("stopDownload")
                .addArgument("dataMethod", dataMethod);
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
    public PendingResult autoReturn() {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("autoReturn");
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
    public PendingResult genericCommand(String commandName, String param) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("genericCommand")
                .addArgument("commandName", commandName)
                .addArgument("param", param);
        return publish(builder.build());
    }

    @Override
    public PendingResult setCamera(CameraName cameraName,
                                   CameraResolution resolution,
                                   float frameRate,
                                   float bandwidth) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setCamera")
                .addArgument("cameraName", cameraName)
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
    public PendingResult setInertia(String name, float mass, Mat33f matrix) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setInertia")
                .addArgument("name", name)
                .addArgument("mass", mass)
                .addArgument("matrix", matrix);
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
    public PendingResult setTelemetryRate(TelemetryType name, float rate) {
        final CommandBuilder builder = makeCommandBuilder();
        builder.setName("setTelemetryRate")
                .addArgument("name", name)
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
