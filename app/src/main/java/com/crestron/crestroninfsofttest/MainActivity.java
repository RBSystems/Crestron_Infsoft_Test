package com.crestron.crestroninfsofttest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.infsoft.android.locator.*;
import com.infsoft.android.maps.MapView;
import com.infsoft.android.maps.MyLocation;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

public class MainActivity extends Activity implements com.infsoft.android.locator.LocationListener, BootstrapNotifier {

    final private int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private LocationManager locationManager;
    private String result = "";
    private String backgroundLocationData = "";
    private int positionNum = 0;
    private RegionBootstrap regionBootstrap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            // Marshmallow
            getAccessFineLocationPermission();
        } else {
            //below Marshmallow
        }

        // get the location manager
        locationManager = LocationManager.getService(this);

        // request location updates
        locationManager.requestLocationUpdates(this, 100, (float)0.2, this);

        //configure mapview
        ((MapView)findViewById(R.id.mapView)).setMinZoomLevel(25);
        ((MapView)findViewById(R.id.mapView)).setMaxZoomLevel(15);
        ((MapView)findViewById(R.id.mapView)).setDisplayButton3D(true);
        ((MapView)findViewById(R.id.mapView)).setDisplayButtonLevelWheel(true);
        ((MapView)findViewById(R.id.mapView)).setDisplayButtonMyPostion(true);

        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
        //TODO Change back to true after testing
        beaconManager.setBackgroundMode(false);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        Identifier identifier = Identifier.parse("F7826DA6-4FA2-4E98-8024-BC5B71E0893E"); //kontakt
        Region region = new Region("com.crestron.crestroninfsoft.MainActivity", null, null, null);
        regionBootstrap = new RegionBootstrap(this,region);
    }

    public void onLocationChanged(Location location) {
        if (location == null || !location.isValid()){
            Toast.makeText(this, "No location found", Toast.LENGTH_SHORT).show();
            return;
        }

        //updates text labels to be current location
        ((TextView)(findViewById(R.id.lattitudeLabel))).setText(String.valueOf(location.getLatitude()));
        ((TextView)(findViewById(R.id.longitudeLabel))).setText(String.valueOf(location.getLongitude()));

        //logs current location for debugging
        Log.d("INDOOR_POSITION", "LATITUDE = " + location.getLatitude());
        Log.d("INDOOR_POSITION", "LONGITUDE = " + location.getLongitude());
        //Log.d("INDOOR_POSITION", "LEVEL = " + location.getLevel());

        //saves the date and location data for further analysis later
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy, HH:mm:ss.SSS");
        String date = df.format(Calendar.getInstance().getTime());
        backgroundLocationData += date;
        backgroundLocationData += ",";
        backgroundLocationData += location.getLatitude();
        backgroundLocationData += ",";
        backgroundLocationData += location.getLongitude();
        backgroundLocationData += ",";

        //update the location on the map
        MyLocation myLocation = new MyLocation(location.getLatitude(), location.getLongitude(), location.getLevel(), location.getAccuracyInMeters(), true);
        ((MapView)findViewById(R.id.mapView)).animateToMyLocation(myLocation);
    }

    @Override
    public void onDestroy() {
        // release location manager
        super.onDestroy();
        locationManager.removeUpdates(this);
    }

    @TargetApi(23)
    private void getAccessFineLocationPermission() {
        int hasAccessFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasAccessFineLocationPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Access Granted
                } else {
                    // Permission Denied
                    // Permission is necessary to calculate the Position
                    // Make user aware of that
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //update the location on the map and the coordinates text label when the user manually refreshes
    public void refreshButtonClick(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

        if (locationManager.getLastKnownLocation() == null || !locationManager.getLastKnownLocation().isValid()){
            Toast.makeText(this, "No location found", Toast.LENGTH_SHORT).show();
            return;
        }

        ((TextView)(findViewById(R.id.lattitudeLabel))).setText(String.valueOf(locationManager.getLastKnownLocation().getLatitude()));
        ((TextView)(findViewById(R.id.longitudeLabel))).setText(String.valueOf(locationManager.getLastKnownLocation().getLongitude()));

        MyLocation myLocation = new MyLocation(locationManager.getLastKnownLocation().getLatitude(),
                locationManager.getLastKnownLocation().getLongitude(), locationManager.getLastKnownLocation().getLevel(),
                locationManager.getLastKnownLocation().getAccuracyInMeters(), true);
        ((MapView)findViewById(R.id.mapView)).animateToMyLocation(myLocation);
    }

    //records the current lattitude and longitude for further analysis
    public void recordButtonClick(View v){
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

        if (locationManager.getLastKnownLocation() == null || !locationManager.getLastKnownLocation().isValid()){
            Toast.makeText(this, "No location found", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(
                this,
                "Position " + (++positionNum) + " recorded at: " + locationManager.getLastKnownLocation().getLatitude() + ","
                        + locationManager.getLastKnownLocation().getLongitude(), Toast.LENGTH_SHORT).show();

        ((TextView)(findViewById(R.id.positionCount))).setText(String.valueOf(positionNum));
        result += locationManager.getLastKnownLocation().getLatitude();
        result += ",";
        result += locationManager.getLastKnownLocation().getLongitude();
        result += ",";
    }

    //emails either the discreet location data that the user has decided to record in the foreground
    //or email the background data that was collected if there are no locations in memory to store
    public void emailBackgroundDataClick(View v){

        //uses the default email client with the message body as the location data in csv format
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setData(Uri.parse("mailto:vwang@crestron.com"));
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Location Data Infsoft");
        sendIntent.putExtra(Intent.EXTRA_EMAIL, "vwang@crestron.com");
        if(!result.equals(""))
            sendIntent.putExtra(Intent.EXTRA_TEXT, result);
        else
            sendIntent.putExtra(Intent.EXTRA_TEXT, backgroundLocationData);
        sendIntent.setType("message/rfc822");
        startActivity(sendIntent);

        //clear out whichever string you used to reset the recording
        if(!result.equals("")) {
            result = "";
            positionNum = 0;
            ((TextView)(findViewById(R.id.positionCount))).setText(String.valueOf(positionNum));
        }
        else{
            backgroundLocationData = "";
        }
    }

    //deletes the most recently recorded point of location data
    public void deleteButtonClick(View v){
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

        int lastComma = result.lastIndexOf(",");
        result = result.substring(0, lastComma);
        lastComma = result.lastIndexOf(",");
        result = result.substring(0, lastComma);
        lastComma = result.lastIndexOf(",");
        result = result.substring(0, lastComma+1);

        Toast.makeText(this, "Position " + Integer.toString(positionNum--) + " deleted", Toast.LENGTH_SHORT).show();
        ((TextView)(findViewById(R.id.positionCount))).setText(String.valueOf(positionNum));
    }

    //start the background location service that records all positions in the background
    public void startServiceClick(View v){
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

        Toast.makeText(getApplicationContext(),"Starting up background beacon monitoring",Toast.LENGTH_LONG).show();

        Identifier identifier = Identifier.parse("F7826DA6-4FA2-4E98-8024-BC5B71E0893E"); //kontakt
        Region region = new Region("com.crestron.crestroninfsoft.MainActivity", null, null, null);
        regionBootstrap = new RegionBootstrap(this,region);

        ((TextView)(findViewById(R.id.backgroundStatus))).setText("ON");
        ((TextView)(findViewById(R.id.backgroundStatus))).setTextColor(0xFF00F708);
    }

    //stops the background location service that records all positions in the background
    public void stopServiceClick(View v){
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

        Toast.makeText(getApplicationContext(),"Shutting down background beacon monitoring",Toast.LENGTH_LONG).show();

        regionBootstrap.disable();

        ((TextView)(findViewById(R.id.backgroundStatus))).setText("OFF");
        ((TextView)(findViewById(R.id.backgroundStatus))).setTextColor(0xFFF90004);
    }

    //called when the app sees a kontakt.io beacon
    @Override
    public void didEnterRegion(Region arg0) {
        Log.d("Beacon Service", "Got a didEnterRegion call");

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Entered Region",
                        Toast.LENGTH_LONG).show();
            }
        });

        // This call to disable will make it so the activity below only gets launched the first time a beacon is seen (until the next time the app is launched)
        // if you want the Activity to launch every single time beacons come into view, remove this call.
        //regionBootstrap.disable();
        Intent intent = new Intent(this, MainActivity.class);
        // IMPORTANT: in the AndroidManifest.xml definition of this activity, you must set android:launchMode="singleInstance" or you will get two instances
        // created when a user launches the activity manually and it gets launched from here.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    //called when the app stops seeing kontakt.io beacons
    @Override
    public void didExitRegion(Region arg0) {
        // Don't care
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Exited Region",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    //called when the app sees a change in the beacon region
    @Override
    public void didDetermineStateForRegion(int arg0, Region arg1) {
        // Don't care
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Region changed",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
