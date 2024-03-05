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


package gov.nasa.arc.irg.astrobee.sci_cam_image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;
import android.util.Size;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.internal.message.RawMessage;
import org.ros.message.MessageFactory;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import ff_msgs.CommandArg;
import ff_msgs.CommandStamped;
import sensor_msgs.CameraInfo;
import sensor_msgs.CompressedImage;
import std_msgs.Header;

public class SciCamPublisher implements NodeMain {

    private boolean mPublishImage;

    private ChannelBufferOutputStream mStream;

    private ConnectedNode mConnectedNode;

    private MessageFactory mMessageFactory;

    private NodeConfiguration mNodeConfig;

    private Publisher<ff_msgs.CommandStamped> mCommandPublisher;
    private Publisher<sensor_msgs.CameraInfo> mCameraInfoPublisher;
    private Publisher<sensor_msgs.CompressedImage> mImagePublisher;

    private ReentrantLock mPublishSettingsLock = new ReentrantLock();

    private static SciCamPublisher instance = new SciCamPublisher();

    private Size mPublishSize;

    private String mPublishType;

    private SciCamPublisher() {
        mPublishImage = false;
        mPublishSize = new Size(640, 480);
        mPublishType = "color";
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Log.d(StartSciCamImage.TAG, "onStart: sci cam publisher starting up!");

        mConnectedNode = connectedNode;

        mCommandPublisher = mConnectedNode.newPublisher("command", CommandStamped._TYPE);

        // Warning: Must keep /compressed at the end of the topic, or else
        // rviz cannot view it!
        NameResolver resolver = connectedNode.getResolver().newChild("hw");
        mImagePublisher =
                mConnectedNode.newPublisher(resolver.resolve("cam_sci/compressed"),
                                            CompressedImage._TYPE);

        mCameraInfoPublisher =
                mConnectedNode.newPublisher(resolver.resolve("cam_sci_info"),
                                            CameraInfo._TYPE);

        mNodeConfig = NodeConfiguration.newPrivate();
        mMessageFactory = mNodeConfig.getTopicMessageFactory();

        mStream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("sci_cam_node");
    }

    public static SciCamPublisher getInstance() {
        return instance;
    }

    @Override
    public void onShutdown(Node node) {
        Log.d(StartSciCamImage.TAG, "onShutdown: Sci cam publisher is shutting down.");
    }

    @Override
    public void onShutdownComplete(Node node) {
        Log.d(StartSciCamImage.TAG, "onShutdownComplete: The sci cam publisher shutdown is complete." );
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        Log.d(StartSciCamImage.TAG, "onError: Sci cam publisher encountered an error.");
    }

    public Bitmap JpegToGrayscale(Bitmap image) {
        Bitmap grayscaleImage = Bitmap.createBitmap(image.getWidth(),
                                                    image.getHeight(),
                                                    Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(grayscaleImage);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(image, 0, 0, paint);
        return grayscaleImage;
    }


    public byte[] processJpeg(byte[] image, Size imageSize) {
        Bitmap processedBitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Log.d(StartSciCamImage.TAG, "processJpeg: image size height: " + imageSize.getHeight());
        Log.d(StartSciCamImage.TAG, "processJpeg: image size width: " + imageSize.getWidth());
        Log.d(StartSciCamImage.TAG, "processJpeg: publish size height: " + mPublishSize.getHeight());
        Log.d(StartSciCamImage.TAG, "processJpeg: publish size width: " + mPublishSize.getWidth());
        if (imageSize.getWidth() != mPublishSize.getWidth() ||
                imageSize.getHeight() != mPublishSize.getHeight()) {
            processedBitmap = Bitmap.createScaledBitmap(processedBitmap,
                                                        mPublishSize.getWidth(),
                                                        mPublishSize.getHeight(),
                                                        false);
        }

        if (mPublishType.equals("grayscale")) {
            processedBitmap = JpegToGrayscale(processedBitmap);
        }

        // Compress image
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        return outputStream.toByteArray();
    }

    public void publishImage(byte[] image, Size imageSize, long msecTimestamp) {
        mPublishSettingsLock.lock();
        Log.d(StartSciCamImage.TAG, "publishImage: Attempting to publish image!");
        long secs = msecTimestamp/1000;
        long nsecs = (msecTimestamp % 1000) * 1000000;

        Time imageTakenTime = new Time((int) secs, (int) nsecs);

        // Publish the camera info
        CameraInfo cameraInfo = mCameraInfoPublisher.newMessage();
        cameraInfo.getHeader().setStamp(imageTakenTime);
        cameraInfo.getHeader().setFrameId("sci_camera");
        cameraInfo.setWidth(mPublishSize.getWidth());
        cameraInfo.setHeight(mPublishSize.getHeight());
        mCameraInfoPublisher.publish(cameraInfo);
        Log.d(StartSciCamImage.TAG, "publishImage: camera info published!");

        if (!mPublishImage) {
            Log.d(StartSciCamImage.TAG, "publishImage: received image but publish is set to false.");
        } else {
            if (mConnectedNode == null) {
                Log.e(StartSciCamImage.TAG, "publishImage: Sci cam publisher node failed to start. Is the ROS master running?");
            } else {
                try {
                    Log.d(StartSciCamImage.TAG, "publishImage: Calling process jpeg image function.");
                    byte[] resizedImage = processJpeg(image, imageSize);

                    // Publish image
                    CompressedImage compressedImage = mImagePublisher.newMessage();
                    compressedImage.setFormat("jpeg");
                    compressedImage.getHeader().setStamp(imageTakenTime);
                    compressedImage.getHeader().setFrameId("sci_camera");
                    mStream.write(resizedImage);
                    compressedImage.setData(mStream.buffer().copy());
                    mStream.buffer().clear();
                    mImagePublisher.publish(compressedImage);

                    Log.d(StartSciCamImage.TAG, "publishImage: camera image published!");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(StartSciCamImage.TAG, "publishImage: exception thrown: " + Log.getStackTraceString(e), e);
                }
            }
        }
        mPublishSettingsLock.unlock();
    }

    public void publishRestartCommand() {
        CommandStamped commandStamped = mCommandPublisher.newMessage();
        Date date = new Date();
        long msecTime = date.getTime();
        long secs = msecTime/1000;
        long nsecs = (msecTime % 1000) * 1000000;
        Time time = new Time((int) secs, (int) nsecs);

        // Set basic command info
        commandStamped.getHeader().setStamp(time);
        commandStamped.setSubsysName("Astrobee");
        commandStamped.setCmdSrc("guest science");
        commandStamped.setCmdOrigin("");
        commandStamped.setCmdName("restartGuestScience");
        commandStamped.setCmdId(("SciCamImage" + secs));

        // Set command arguments, first is the apk name, second is the wait time
        List<CommandArg> argsList = commandStamped.getArgs();
        CommandArg apkCommandArg = mMessageFactory.newFromType(CommandArg._TYPE);
        // Data type 5 means string
        byte strByte = 5;
        apkCommandArg.setDataType(strByte);
        apkCommandArg.setS("gov.nasa.arc.irg.astrobee.sci_cam_image");
        argsList.add(apkCommandArg);

        CommandArg waitCommandArg = mMessageFactory.newFromType(CommandArg._TYPE);
        // Data type 3 means int
        byte intByte = 3;
        waitCommandArg.setDataType(intByte);
        // Set wait to 10 seconds
        waitCommandArg.setI(10);
        argsList.add(waitCommandArg);

        commandStamped.setArgs(argsList);

        // Publish command
        mCommandPublisher.publish(commandStamped);
    }

    public void setPublishImage(boolean pub) {
        mPublishSettingsLock.lock();
        mPublishImage = pub;
        mPublishSettingsLock.unlock();
    }

    public boolean setPublishSize(Size size) {
        if (size.getWidth() > 0 && size.getHeight() > 0) {
            mPublishSettingsLock.lock();
            mPublishSize = size;
            mPublishSettingsLock.unlock();
            return true;
        }
        return false;
    }

    public boolean setPublishType(String type) {
        if (type.equals("color") || type.equals("grayscale")) {
            mPublishSettingsLock.lock();
            mPublishType = type;
            mPublishSettingsLock.unlock();
            return true;
        }
        return false;
    }
}
