

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

package gov.nasa.arc.irg.astrobee.picoflexx;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

class UIHandler extends Handler {
    private Handler.Callback mCallback;

    UIHandler(final Handler.Callback callback) {
        super(Looper.getMainLooper());
        mCallback = callback;
    }

    UIHandler() {
        this(null);
    }

    void setCallback(final Handler.Callback callback) {
        mCallback = callback;
    }

    @Override
    public void handleMessage(final Message msg) {
        boolean handled = false;
        if (mCallback != null) {
            handled = mCallback.handleMessage(msg);
        }

        if (!handled)
            super.handleMessage(msg);
    }
}
