package gov.nasa.arc.astrobee.android.gs;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by kmbrowne on 11/21/17.
 *
 * InfoInqueryBroadcastReceiver - Responsible for gathering all the information that the guest
 * science manager needs and sending it to the gs manager when requested.
 */

public class InfoInqueryBroadcastReceiver extends BroadcastReceiver {
    /**
     * onReceiver - This method is called when the guest science manager sends an intent broadcast to
     * collect guest science apk information.
     *
     * @param context - Context that the receiver is running in.
     * @param intent - The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle results = getResultExtras(true);

        // Parse commands and put them in a bundle
        Bundle info = parseCommands(context);
        // If unable to parse commands and apk information, log error and return.
        if (info == null) {
            Log.e("GuestScienceLib", "The library was unable to extract the apk command info.");
            return;
        }

        // Make pending intent and add it to apk info
        PendingIntent startIntent = extractStartService(context);
        // If the start intent is null, report an error since the guest science manager can't start
        // a guest science apk if it doesn't have a pending intent to start.
        if (startIntent == null) {
            Log.e("GuestScienceLib", "The library was unable to extract the service to start " +
                    "upon receiving a start guest science command.");
            return;
        }

        // Add pending intent to bundle that gets returned to the guest science manager
        info.putParcelable("startIntent", startIntent);

        // Use get package to get full name
        String fullName = context.getApplicationContext().getPackageName();
        // Make sure the full apk name is not blank
        if (fullName.length() == 0) {
            Log.e("GuestScienceLib", "The library was unable to extract the full apk name.");
            return;
        }

        // Put bundle in results for the guest science manager
        results.putBundle(fullName, info);
    }

    /**
     * extractStartService - This function parses the Android manifest to find the service to start.
     * This service should have meta data tag specifying that it is the service to start.
     *
     * @param context - Context that the receiver is running in.
     * @return - The pending intent that starts the apk.
     */
    public PendingIntent extractStartService(Context context) {
        PendingIntent pendingIntent = null;
        String packageName = context.getApplicationContext().getPackageName();
        String serviceName = "";
        PackageManager packageManager = context.getPackageManager();
        try {
            // Get package info but only the service information and meta data
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    packageName,
                    (PackageManager.GET_META_DATA | PackageManager.GET_SERVICES));
            ServiceInfo[] services = packageInfo.services;
            // Go through services to find the one with the right meta data
            for (ServiceInfo serviceInfo : services) {
                // Not every service has to have meta data
                if (serviceInfo.metaData != null) {
                    // Check if the meta data contains the key we are looking for
                    if (serviceInfo.metaData.containsKey("Start Service")) {
                        // Make sure it is set to true
                        if (serviceInfo.metaData.getBoolean("Start Service", false)) {
                            serviceName = serviceInfo.name;
                        }
                    }
                }
            }

            // If we weren't able to extract the service name, report an error.
            if (serviceName.length() == 0) {
                Log.e("GuestScienceLib", "The library was unable to find start service in the " +
                        "AndroidManifest.xml file.");
                return null;
            }

            // Create a pending intent using the service name we found
            ComponentName componentName = ComponentName.createRelative(packageName, serviceName);
            Intent startIntent = new Intent().setComponent(componentName);
            pendingIntent = PendingIntent.getService(context, 0, startIntent, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("GuestScienceLib", e.getMessage(), e);
        }
        return pendingIntent;
    }

    /**
     * parseCommands - This function parses the commands.xml file to extract the custom commands the
     * apk accepts. It also extracts whether the apk is primary or not and the short name if
     * specified.
     *
     * @param context - Context that receiver is running in.
     * @return - A bundle containing the apk information.
     */
    public Bundle parseCommands(Context context) {
        Bundle apkInfo = new Bundle();
        ArrayList<String> commands = new ArrayList<String>();
        String tagName, commandName, commandSyntax = "", primaryStr = "";
        boolean primary = false;

        Resources resources = context.getResources();

        // Get xml parser
        XmlResourceParser parser = resources.getXml(R.xml.commands);

        // Get application name and use it as the short name
        String shortName = resources.getString(R.string.app_name);
        apkInfo.putString("shortName", shortName);

        try {
            // Check for end of document
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // If event type is start tag, check to see if it is a needed tag
                if (eventType == XmlPullParser.START_TAG) {
                    tagName = parser.getName();
                    if (tagName.contentEquals("shortName")) {
                        eventType = parser.next();
                        // Make sure a short name was actually specified as text
                        if (eventType == XmlPullParser.TEXT) {
                            shortName = parser.getText();
                            apkInfo.putString("shortName", shortName);
                        }
                    } else if (tagName.contentEquals("primary")) {
                        eventType = parser.next();
                        if (eventType == XmlPullParser.TEXT) {
                            primaryStr = parser.getText();
                            // Convert string to lower case so don't have to compare against True,
                            // TRUE, etc
                            primaryStr.toLowerCase();
                            // Make sure the value was either true or false
                            if (primaryStr.contentEquals("true")) {
                                primary = true;
                            } else if (!primaryStr.contentEquals("false")) {
                                Log.e("GuestScienceLib", " Primary needs to be either true or " +
                                        "false in the commands.xml file. It will be set to the " +
                                        "default which is false");
                            }
                            apkInfo.putBoolean("primary", primary);
                        }
                    } else if (tagName.contentEquals("command")) {
                        // Extract command name and syntax
                        commandName = parser.getAttributeValue(null, "name");
                        commandSyntax = parser.getAttributeValue(null, "syntax");
                        if (commandName != null && commandSyntax != null) {
                            // Add command information to both a commands array and the bundle. The
                            // commands array will be added to the bundle after all the commands
                            // have been added. This was done because Android discourages nesting
                            // bundles. So the guest science manager will extract the commands array
                            // from the bundle, go through it, and extract the command syntax
                            // portion out of the bundle.
                            commands.add(commandName);
                            apkInfo.putString(commandName, commandSyntax);
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e("GuestScienceLib", e.getMessage(), e);
        }

        // Add commands array to the bundle so that the guest science manager can use it to extract
        // the command syntax out of the bundle
        apkInfo.putStringArrayList("commands", commands);
        return apkInfo;
    }
}