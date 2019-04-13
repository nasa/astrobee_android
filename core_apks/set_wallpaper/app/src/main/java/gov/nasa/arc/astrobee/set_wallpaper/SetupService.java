package gov.nasa.arc.astrobee.set_wallpaper;

import android.app.IntentService;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

import java.io.IOException;

/**
 * Created by kmbrowne on 11/29/18.
 */

public class SetupService extends IntentService {
    public SetupService() {
        super("SetupService");
    }

    protected void onHandleIntent(final Intent intent) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.astrobee_logo_modified_background);
        WallpaperManager manager = WallpaperManager.getInstance(getApplicationContext());

        Rect cropHints = new Rect(630, 0, 2250, 1620);

        try {
            manager.setBitmap(bitmap, cropHints, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
