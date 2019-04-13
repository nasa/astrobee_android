

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

package gov.nasa.arc.irg.astrobee.mictest;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;

    private Button mBtnPlay;
    private Button mBtnRecord;
    private Button mBtnPlayback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnPlay = (Button) findViewById(R.id.main_btn_play);
        mBtnRecord = (Button) findViewById(R.id.main_btn_record);
        mBtnPlayback = (Button) findViewById(R.id.main_btn_playback);
    }

    private boolean mRecording = false;
    private boolean mPlaying = false;

    private final MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.i(TAG, "Finished playing media, destroying player");
            mPlayer.release();
            mPlayer = null;
            mPlaying = false;
            setState(STATE_IDLE);
        }
    };

    public void onPlayClick(View v) {
        if (!mPlaying) {
            try {
                mPlayer = new MediaPlayer();
                AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.astrobee);
                mPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                mPlayer.setOnCompletionListener(mOnCompletionListener);
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.prepare();
                mPlayer.start();
                mPlaying = true;
                setState(STATE_PLAYING);
            } catch (IOException e) {
                Log.e(TAG, "error trying to play a sound");
            }
        } else {
            mPlayer.setOnCompletionListener(null);
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
            mPlaying = false;
            setState(STATE_IDLE);
        }
    }

    public void onRecordClick(View v) {
        if (!mRecording) {
            File f = new File(getExternalFilesDir(null), "recording.mp4");

            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setOutputFile(f.getAbsolutePath());
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD);
            mRecorder.setAudioSamplingRate(48000);
            mRecorder.setAudioEncodingBitRate(96000);

            try {
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e(TAG, "unable to prepare MediaRecorder");
                mRecorder = null;
                return;
            }

            mRecorder.start();
            mRecording = true;

            setState(STATE_RECORDING);
        } else {
            mRecorder.stop();
            mRecorder.release();
            mRecording = false;

            setState(STATE_IDLE);
        }
    }

    public void onPlaybackClick(View v) {
        File f = new File(getExternalFilesDir(null), "recording.mp4");

        if (!mPlaying) {
            try {
                mPlayer = new MediaPlayer();
                mPlayer.setDataSource(f.getAbsolutePath());
                mPlayer.setOnCompletionListener(mOnCompletionListener);
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.prepare();
                mPlayer.start();
                mPlaying = true;
                setState(STATE_PLAYING_BACK);
            } catch (IOException e) {
                Log.e(TAG, "error trying to playback recording");
            }
        } else {
            mPlayer.setOnCompletionListener(null);
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;

            mPlaying = false;
            setState(STATE_IDLE);
        }
    }

    private static final int STATE_IDLE = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_RECORDING = 2;
    private static final int STATE_PLAYING_BACK = 3;

    private void setState(int state) {
        mBtnPlay.setEnabled(false);
        mBtnRecord.setEnabled(false);
        mBtnPlayback.setEnabled(false);

        switch (state) {
        case STATE_IDLE:
            mBtnPlay.setText(R.string.main_play_bundle);
            mBtnPlay.setEnabled(true);

            mBtnRecord.setText(R.string.main_record);
            mBtnRecord.setEnabled(true);

            mBtnPlayback.setText(R.string.main_playback);
            mBtnPlayback.setEnabled(true);
            break;
        case STATE_PLAYING:
            mBtnPlay.setText(R.string.main_stop);
            mBtnPlay.setEnabled(true);
            break;
        case STATE_RECORDING:
            mBtnRecord.setText(R.string.main_stop);
            mBtnRecord.setEnabled(true);
            break;
        case STATE_PLAYING_BACK:
            mBtnPlayback.setText(R.string.main_stop);
            mBtnPlayback.setEnabled(true);
        }
    }
}
