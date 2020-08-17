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

import android.graphics.ImageFormat;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.hardware.Camera;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Runnable;
import java.lang.Thread;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * Publish full-resolution images from the camera and a topic with camera information.
 */
public class SciCamPublisher implements NodeMain {
    
  private ConnectedNode connectedNode;
  private Publisher<sensor_msgs.CompressedImage> imagePublisher;
  private Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;
  private ChannelBufferOutputStream stream;
  public SciCamPublisher() {
  }
    
  @Override
      public void onStart(ConnectedNode connectedNode) {
      
       if (SciCamImage.doLog)
           Log.i(SciCamImage.SCI_CAM_TAG, "Starting SciCamPublisher and setting up the publishers.");

      this.connectedNode = connectedNode;

      // Warning: Must keep /compressed at the end of the topic, or else
      // rviz cannot view it!
      NameResolver resolver = connectedNode.getResolver().newChild("hw");
      imagePublisher =
          connectedNode.newPublisher(resolver.resolve("cam_sci/compressed"),
                                     sensor_msgs.CompressedImage._TYPE);
      cameraInfoPublisher =
          connectedNode.newPublisher(resolver.resolve("cam_sci_info"),
                                     sensor_msgs.CameraInfo._TYPE);
      stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
  }

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("sci_cam_node");
  }

  @Override
  public void onShutdown(Node node) {
       if (SciCamImage.doLog)
           Log.i(SciCamImage.SCI_CAM_TAG, "SciCamPublisher shutdown.");
  }

  @Override
  public void onShutdownComplete(Node node) {
       if (SciCamImage.doLog)
           Log.i(SciCamImage.SCI_CAM_TAG, "SciCamPublisher shutdown complete.");
  }

  @Override
  public void onError(Node node, Throwable throwable) {
       if (SciCamImage.doLog)
           Log.i(SciCamImage.SCI_CAM_TAG, "SciCamPublisher error.");
  }

public void onNewRawImage(byte[] data, Size size) {

     if (SciCamImage.doLog)
         Log.i(SciCamImage.SCI_CAM_TAG, "onNewRawImage: image size is: " + size.width + " x " + size.height);
    
    try {

        if (connectedNode == null) {
             if (SciCamImage.doLog)
                 Log.i(SciCamImage.SCI_CAM_TAG, "SciCamPublisher failed to start. Is the ROS master running?");
            return;
        }
        
        Time currentTime = connectedNode.getCurrentTime();
        String frameId = "camera";
        
        // Publish the image
        sensor_msgs.CompressedImage image = imagePublisher.newMessage();
        image.setFormat("jpeg");
        image.getHeader().setStamp(currentTime);
        image.getHeader().setFrameId(frameId);
        stream.write(data);
        image.setData(stream.buffer().copy());
        stream.buffer().clear();
        imagePublisher.publish(image);
        
        // Publish the camera info
        sensor_msgs.CameraInfo cameraInfo = cameraInfoPublisher.newMessage();
        cameraInfo.getHeader().setStamp(currentTime);
        cameraInfo.getHeader().setFrameId(frameId);
        cameraInfo.setWidth(size.width);
        cameraInfo.setHeight(size.height);
        cameraInfoPublisher.publish(cameraInfo);
        
    } catch (Exception e) {
         if (SciCamImage.doLog)
             Log.i(SciCamImage.SCI_CAM_TAG, "onNewRawImage: exception thrown: " + Log.getStackTraceString(e));
    }
  }    
}
