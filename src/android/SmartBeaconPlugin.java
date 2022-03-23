package com.tcd.iot.smartbeacon;


import android.Manifest;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SmartBeaconPlugin extends CordovaPlugin implements MonitorNotifier {
    private BeaconManager beaconManager;
    private static final String TAG = "CustomBeaconScanner";
    public static final Region wildcardRegion = new Region("wildcardRegion", Identifier.parse("A495BB10-C5B1-4B44-B512-1370F02D74DE"), null, null);


    class SmartBeacon {
        public String uuid;
        public String major;
        public String minor;
        public double distance;
        public long timestamp;
        public int rssi;
        public int txPower;


        public JSONObject toJSONObject() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("uuid", this.uuid);
                obj.put("major", this.major);
                obj.put("minor",this.minor);
                obj.put("distance", this.distance);
                obj.put("timestamp", this.timestamp);
                obj.put("rssi", this.rssi);
                obj.put("txPower", this.txPower);
                return obj;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private HashMap<String, SmartBeacon> beaconsMap = new HashMap<String, SmartBeacon>();

    @Override
    protected void pluginInitialize() {
            Context context = cordova.getActivity().getApplicationContext();
            Log.i(TAG, "Beginning plugin initialization phase ");
            if(BeaconManager.getInstanceForApplication(context).checkAvailability()){
                cordova.requestPermission(this, 1, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                cordova.requestPermission(this, 2, Manifest.permission.ACCESS_FINE_LOCATION);
                Log.i(TAG, "Beginning Scanning of beacons");
                BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
                beaconManager = BeaconManager.getInstanceForApplication(context);
                beaconManager.getBeaconParsers().add(new BeaconParser().
                        setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
                beaconManager.addMonitorNotifier(this);
                beaconManager.startMonitoring(wildcardRegion);
            }
        }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scan")) {
            Log.i(TAG, "In execute method of custom plugin" );
            //JSONObject options = args.getJSONObject(0);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, toJSONArray(beaconsMap));
            callbackContext.sendPluginResult(pluginResult);
        }
        else
        {
            return false;
        }
        return true;
    }


    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
    }
    @Override
    public void didEnterRegion(Region region) {
        this.sendNotification();
    }

    private void sendNotification() {
        Log.d(TAG, "Calculating distance from beacons");
        RangeNotifier rangeNotifier = (beacons, region) -> {
            if (beacons.size() > 0) {
                for(Beacon beacon:beacons){
                    SmartBeacon smartBeacon = new SmartBeacon();
                    smartBeacon.distance = beacon.getDistance();
                    smartBeacon.uuid = beacon.getId1().toString();

                    smartBeacon.major = beacon.getId2().toString();
                    smartBeacon.minor = beacon.getId3().toString();
                    Log.d(TAG, "I see a beacon with an url: " + smartBeacon.minor +
                            " at " + beacon.getDistance() + " meters away.");
                    smartBeacon.rssi = beacon.getRssi();
                    smartBeacon.txPower = beacon.getTxPower();
                    this.beaconsMap.put(smartBeacon.minor,smartBeacon);
                }
                //logToDisplay("The first beacon " + firstBeacon.toString() + " is about " + firstBeacon.getDistance() + " meters away.");
            }
        };
        beaconManager.addRangeNotifier(rangeNotifier);
        beaconManager.startRangingBeacons(wildcardRegion);
    }

    @Override
    public void didExitRegion(Region region) {

    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {

    }

    private JSONArray toJSONArray(Map<String,SmartBeacon> beaconMap){
        JSONArray array = new JSONArray();
        beaconMap.entrySet().stream().forEach(i -> array.put(i.getValue().toJSONObject()));
        return array;
    }
}