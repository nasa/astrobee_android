

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

import android.content.Context;
import android.content.SharedPreferences;

import java.net.URI;

public final class Settings {
    static final String KEY_MASTER_URI = "master_uri";
    static final String KEY_HOSTNAME = "hostname";

    static Settings sInstance = null;
    static Settings getInstance() {
        if (sInstance == null)
            throw new IllegalStateException("Settings is null?");
        return sInstance;
    }
    static void ensureInstance(final Context ctx) {
        if (sInstance != null)
            return;
        sInstance = new Settings(ctx);
    }

    private final Context mContext;

    private Settings(final Context ctx) {
        mContext = ctx;
    }

    final SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(
                mContext.getPackageName() + "_preferences",
                Context.MODE_PRIVATE);
    }

    private String getString(final String key, final int id) {
        final SharedPreferences prefs = getSharedPreferences();
        return prefs.getString(key, mContext.getResources().getString(id));
    }

    final URI getRosMasterUri() {
        return URI.create(getString(KEY_MASTER_URI, R.string.default_master_uri));
    }

    final String getHostname() {
        return getString(KEY_HOSTNAME, R.string.default_hostname);
    }
}
