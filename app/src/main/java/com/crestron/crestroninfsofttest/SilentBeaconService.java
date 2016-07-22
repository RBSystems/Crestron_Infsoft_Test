package com.crestron.crestroninfsofttest;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.Iterator;


public class SilentBeaconService extends Service implements BeaconConsumer {
    private BeaconManager beaconManager;

    @Override
    public void onCreate() {
        Toast.makeText(this,"Background beacon scanning service has been started", Toast.LENGTH_LONG).show();
        super.onCreate();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        //TODO Change back to true after testing
        beaconManager.setBackgroundMode(false);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);

    }

    @Override
    public void onDestroy() {
        Toast.makeText(this,"Background beacon scanning service has been destroyed", Toast.LENGTH_LONG).show();
        try {
            Thread.sleep(1000);
        }
        catch (Exception e){
            //only for testing
        }
        beaconManager.unbind(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.d("Beacon Service", "I just saw an beacon for the first time!");
                Log.d("Beacon Service", "Region id - " + region.getUniqueId());
                Toast.makeText(getApplicationContext(), "omg saw a beacon", Toast.LENGTH_LONG).show();

            }

            @Override
            public void didExitRegion(Region region) {
                Log.d("Beacon Service", "I no longer see a beacon");
                Log.d("Beacon Service", "Region id - " + region.getUniqueId());
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
            }
        });

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Iterator<Beacon> beaconIterator = beacons.iterator();
                    while (beaconIterator.hasNext()) {
                        Beacon beacon = beaconIterator.next();
                        //check if beacon exists in our DB and throw notification
                        Toast.makeText(getApplicationContext(),"beacon major: "+beacon.getId2()+" beacon minor: "+beacon.getId3(),Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        try {
            Toast.makeText(this,"Starting to scan for kontakt.io beacons", Toast.LENGTH_LONG).show();
            Identifier identifier = Identifier.parse("F7826DA6-4FA2-4E98-8024-BC5B71E0893E"); //kontakt
            beaconManager.startMonitoringBeaconsInRegion(new Region("Kontakt.io beacon", identifier, null, null));
            beaconManager.startRangingBeaconsInRegion(new Region("Kontakt.io beacon", null, null, null));
        } catch (RemoteException e) {
            Log.e("Beacon Service", e.getMessage(), e);
        }
    }


}
