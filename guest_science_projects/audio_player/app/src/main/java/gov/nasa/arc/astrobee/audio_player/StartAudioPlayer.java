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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.ros.address.InetAddressFactory;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Date;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

public class StartAudioPlayer extends StartGuestScienceService {
    public static final String TAG = "AudioPlayer";

    private AudioManager mAudioManager = null;

    private AudioTimestampPublisher mAudioTimestampPublisher = null;

    private int mCurrentVolume = 0;
    private int mMaxVolume = 0;

    private MediaPlayer mPlayer = null;

    private final MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            Log.d(TAG, "Finished playing audio!");
            sendData(MessageType.JSON, "info", "{\"Summary\": Finished playing audio!\"}");
        }
    };

    private NodeConfiguration mNodeConfiguration = null;

    private NodeMainExecutor mNodeMainExecutor = null;

    private String mAudioFilePath = "";

    @Override
    public void onGuestScienceCustomCmd(String command) {
        // Inform GDS/the ground that this apk received a command
        sendReceivedCustomCommand("info");

        Log.d(TAG, "onGuestScienceCustomCmd: Received cmd " + command);

        String commandResult = "{\"Summary\": ";

        try {
            JSONObject obj = new JSONObject(command);
            String commandName = obj.getString("name");

            switch (commandName) {
                case "decreaseVolume":
                    Log.d(TAG, "Received decrease volume command.");
                    if (setVolume((mCurrentVolume - 1))) {
                        commandResult += "Volume decreased to " + mCurrentVolume + ".\"}";
                    } else {
                        commandResult += "Unable to decrease volume. Volume set to " + mCurrentVolume + ".\"}";
                    }
                    break;
                case "increaseVolume":
                    Log.d(TAG, "Received increase volume command.");
                    if (setVolume((mCurrentVolume + 1))) {
                        commandResult += "Volume increased to " + mCurrentVolume + ".\"}";
                    } else {
                        commandResult += "Unable to increase volume. Volume set to " + mCurrentVolume + ".\"}";
                    }
                    break;
                case "playSound":
                    Log.d(TAG, "Received play sound command.");
                    if (obj.has("file")) {
                        String file = mAudioFilePath + File.separator + obj.getString("file");

                        // Extract volume from the command if it exists.
                        // If it doesn't exist, continue with the current volume
                        boolean volumeSet = true;
                        int volume = 0;
                        if (obj.has("volume")) {
                            volume = obj.getInt("volume");
                            volumeSet = setVolume(volume);
                        }

                        // Extract loop from the command if it exists.
                        // If it doesn't, looping should be false.
                        boolean looping = false;
                        if (obj.has("loop")) {
                            looping = obj.getBoolean("loop");
                            Log.d(TAG, "looping is set to " + looping);
                        }
                        if (mPlayer != null && mPlayer.isPlaying()) {
                            commandResult += "Received play sound command but we are already playing audio.\"}";
                        } else {
                            if (volumeSet) {
                                try {
                                    Log.d(TAG, "onGuestScienceCustomCmd: file is " + file);
                                    FileInputStream fileInputStream = new FileInputStream(file);
                                    FileDescriptor fileDescriptor = fileInputStream.getFD();
                                    mPlayer = new MediaPlayer();
                                    mPlayer.setOnCompletionListener(mOnCompletionListener);
                                    mPlayer.setDataSource(fileDescriptor);
                                    mPlayer.setLooping(looping);
                                    mPlayer.prepare();
                                    mPlayer.start();
                                    Date date = new Date();
                                    long startAudioTimestamp = date.getTime();
                                    mAudioTimestampPublisher.sendStartAudioTimestamp(file, startAudioTimestamp);
                                    commandResult += "Playing audio file " + file + ".\"}";
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                    commandResult += "Error: Encountered file not found exception.\"}";
                                } catch (SecurityException e) {
                                    e.printStackTrace();
                                    commandResult += "Error: Encountered security exception.\"}";
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    commandResult += "Error: Encountered an exception.\"}";
                                }
                            } else {
                                commandResult += "Unable to set volume to " + volume + ". Volume set to " + mCurrentVolume + ".\"}";
                            }
                        }
                    } else {
                        commandResult += "Error: File argument not provided in play sound command.\"}";
                    }
                    break;
                case "setVolume":
                    Log.d(TAG, "Received set volume command.");
                    if (obj.has("volume")) {
                        int volume = obj.getInt("volume");
                        if (setVolume(volume)) {
                            commandResult += "Volume set to " + mCurrentVolume + ".\"}";
                        } else {
                            commandResult += "Unable to set volume to " + volume + ". Volume set to " + mCurrentVolume + ".\"}";
                        }
                    } else {
                        commandResult += "Error: Volume argument not provided in set volume command.\"}";
                    }
                    break;
                case "stopSound":
                    Log.d(TAG, "Received stop sound command.");
                    if (mPlayer != null && mPlayer.isPlaying()) {
                            mPlayer.stop();
                            commandResult += "Stopped playing audio file.";
                    } else {
                        commandResult += "Cannot stop sound because nothing is being played.";
                    }
                    break;
                default:
                    commandResult += "\"Commmand " + commandName + " not recognized!\"}";
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            commandResult += "\"Error parsing JSON!\"}";
        }
        sendData(MessageType.JSON, "info", commandResult);
        Log.i(TAG, commandResult);
    }

    @Override
    public void onGuestScienceStart () {
        Log.d(TAG, "onGuestScienceStart: Starting up the audio player.");

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager == null) {
            Log.e(TAG, "onGuestScienceStart: The audio manager is null. This apk won't work.");
            return;
        }

        mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        Log.d(TAG, "onGuestScienceStart: The current volume is " + mCurrentVolume);
        Log.d(TAG, "onGuestScienceStart: The max volume is " + mMaxVolume);

        mAudioFilePath = getGuestScienceDataBasePath();
        if (mAudioFilePath.equals("")) {
            mAudioFilePath = "/sdcard/data/gov.nasa.arc.astrobee.audio_player/incoming";
        } else {
            mAudioFilePath += File.separator + "incoming";
        }

        try {
            URI masterURI = new URI("http://llp:11311");

            mNodeConfiguration = NodeConfiguration
                    .newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            mNodeConfiguration.setMasterUri(masterURI);

            mNodeMainExecutor = DefaultNodeMainExecutor.newDefault();

            mAudioTimestampPublisher = AudioTimestampPublisher.getInstance();

            mNodeMainExecutor.execute(mAudioTimestampPublisher, mNodeConfiguration);
            Log.d(TAG, "Started ROS!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to start the ros connection!", e);
            sendData(MessageType.JSON, "error", "{\"Summary\": \"Failed to start the ros connection!\"}");
            return;
        }

        sendStarted("info");
        Log.i(TAG, "onGuestScienceStart: Audio player apk started up successfully!");
    }

    @Override
    public void onGuestScienceStop () {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
        }

        // Inform GDS/the ground
        sendStopped("info");
        Log.d(TAG, "onGuestScienceStop: Shutting down.");

        stopForeground(true);
        stopSelf();

        // Destroy all connection with the guest science manager
        terminate();
    }

    public boolean setVolume(int volume) {
        if (volume > mMaxVolume) {
            volume = mMaxVolume;
            Log.i(TAG, "setVolume: Volume was greater than max so it was set to max.");
        } else if (volume < 0) {
            volume = 0;
            Log.i(TAG, "setVolume: Volume was less than 0 so it was set to 0.");
        }
        mCurrentVolume = volume;
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (streamVolume != mCurrentVolume) {
            Log.e(TAG, "setVolume: Unable to set volume to " + mCurrentVolume + ". Volume set to " + streamVolume + ".");
            mCurrentVolume = streamVolume;
            return false;
        } else {
            Log.d(TAG, "setVolume: Volume set to " + volume + ".");
        }
        return true;
    }
}