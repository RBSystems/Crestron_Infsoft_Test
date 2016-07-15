package com.crestron.crestroninfsofttest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.infsoft.android.locator.*;
import com.infsoft.android.maps.MapView;
import com.infsoft.android.maps.MyLocation;

public class MainActivity extends Activity implements com.infsoft.android.locator.LocationListener {

    final private int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private LocationManager locationManager;
    private String result = "";
    private int positionNum = 0;

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
        locationManager.requestLocationUpdates(this, 100, (float)0.3, this);

        //configure mapview
        ((MapView)findViewById(R.id.mapView)).setMinZoomLevel(25);
        ((MapView)findViewById(R.id.mapView)).setMaxZoomLevel(15);
        ((MapView)findViewById(R.id.mapView)).setDisplayButton3D(true);
        ((MapView)findViewById(R.id.mapView)).setDisplayButtonLevelWheel(true);
        ((MapView)findViewById(R.id.mapView)).setDisplayButtonMyPostion(true);

    }

    public void onLocationChanged(Location location) {
        if (location == null || !location.isValid()){
            Toast.makeText(this, "No location found", Toast.LENGTH_SHORT).show();
            return;
        }

        ((TextView)(findViewById(R.id.lattitudeLabel))).setText(String.valueOf(location.getLatitude()));
        ((TextView)(findViewById(R.id.longitudeLabel))).setText(String.valueOf(location.getLongitude()));

        Log.d("INDOOR_POSITION", "LATITUDE = " + location.getLatitude());
        Log.d("INDOOR_POSITION", "LONGITUDE = " + location.getLongitude());
        Log.d("INDOOR_POSITION", "LEVEL = " + location.getLevel());

        MyLocation myLocation = new MyLocation(location.getLatitude(), location.getLongitude(), location.getLevel(), 10, true);
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

    public void refreshButtonClick(View v) {

        if (locationManager.getLastKnownLocation() == null || !locationManager.getLastKnownLocation().isValid()){
            Toast.makeText(this, "No location found", Toast.LENGTH_SHORT).show();
            return;
        }

        ((TextView)(findViewById(R.id.lattitudeLabel))).setText(String.valueOf(locationManager.getLastKnownLocation().getLatitude()));
        ((TextView)(findViewById(R.id.longitudeLabel))).setText(String.valueOf(locationManager.getLastKnownLocation().getLongitude()));

        MyLocation myLocation = new MyLocation(locationManager.getLastKnownLocation().getLatitude(),
                locationManager.getLastKnownLocation().getLongitude(), locationManager.getLastKnownLocation().getLevel(), 10, true);
        ((MapView)findViewById(R.id.mapView)).animateToMyLocation(myLocation);
    }

    public void recordButtonClick(View v){
        if (locationManager.getLastKnownLocation() == null || !locationManager.getLastKnownLocation().isValid()){
            Toast.makeText(this, "No location found", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(
                this,
                "Position " + (++positionNum) + " recorded at: " + locationManager.getLastKnownLocation().getLatitude() + ","
                        + locationManager.getLastKnownLocation().getLongitude(), Toast.LENGTH_SHORT).show();

        result += locationManager.getLastKnownLocation().getLatitude();
        result += ",";
        result += locationManager.getLastKnownLocation().getLongitude();
        result += ",";
    }

    public void emailButtonClick(View v){
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setData(Uri.parse("mailto:vwang1111@gmail.com"));
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Location Data Infsoft");
        sendIntent.putExtra(Intent.EXTRA_EMAIL, "vwang1111@gmail.com");
        sendIntent.putExtra(Intent.EXTRA_TEXT, result);
        sendIntent.setType("message/rfc822");
        startActivity(sendIntent);
        result = "";
        positionNum = 0;
    }

    public void startServiceClick(View v){
        startService(new Intent(MainActivity.this, SilentBeaconService.class));
    }

    public void stopServiceClick(View v){
        stopService(new Intent(MainActivity.this, SilentBeaconService.class));
    }
}
