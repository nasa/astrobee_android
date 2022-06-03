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


package gov.nasa.arc.astrobee.audio_player;

import android.util.Log;

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import std_msgs.Header;

public class AudioTimestampPublisher implements NodeMain {

    private static AudioTimestampPublisher instance = new AudioTimestampPublisher();

    private ConnectedNode mConnectedNode;

    private org.apache.commons.logging.Log mLogger;

    private Publisher<Header> mAudioTimestampPublisher;

    private AudioTimestampPublisher() {
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Log.d(StartAudioPlayer.TAG, "onStart: The audio player publisher starting up!");

        mConnectedNode = connectedNode;

        mAudioTimestampPublisher = mConnectedNode.newPublisher("gs/audio_player/timestamp",
                                                               Header._TYPE);

        mLogger = mConnectedNode.getLog();
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("audio_player");
    }

    public static AudioTimestampPublisher getInstance() {
        return instance;
    }

    @Override
    public void onShutdown(Node node) {
        Log.d(StartAudioPlayer.TAG, "onShutdown: The audio player publisher is shutting down.");
    }

    @Override
    public void onShutdownComplete(Node node) {
        Log.d(StartAudioPlayer.TAG, "onShutdown: The audio player publisher shutdown is complete.");
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        Log.d(StartAudioPlayer.TAG, "onError: The audio player publisher encountered an error.");
    }

    public void sendStartAudioTimestamp(String audioFilename, long timestamp) {
        long secs = timestamp/1000;
        long nsecs = (timestamp % 1000) * 1000000;
        Time audioStartedTime = new Time((int) secs, (int) nsecs);

        Header header = mAudioTimestampPublisher.newMessage();
        header.setStamp(audioStartedTime);
        header.setFrameId(audioFilename);

        String msg = "Started playing audio file " + audioFilename + " at " + timestamp;
        mLogger.info(msg);
        mAudioTimestampPublisher.publish(header);
    }
}