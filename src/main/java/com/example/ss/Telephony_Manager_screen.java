package com.example.ss;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

public class Telephony_Manager_screen extends Activity {

/**
* Member variables for storing the telephony and location data
**/
private int SS_val = 0;
private int SS_in_dbm_val = 0;
private int UTRAN_CID_val = 0;
private int CID_val = 0;
private int RNC_ID_val = 0;
private int MCC_val = 0;
private int MNC_val = 0;
private int PSC_val = 0;
private int LAC_val = 0;
private String IP_val = "";
private double lat_val = 0;
private double log_val = 0;
private double alt_val = 0;
private double bearing_val = 0;
private double accuracy_val = 0;
private double speed_val = 0;
private String event_val = "";

/**
* Member variables for UI of the telephony data
**/
private TextView Signal_Strength;
private TextView Signal_Strength_dbm;
private TextView UTRAN_CID;
private TextView CID;
private TextView RNC_ID;
private TextView MCC;
private TextView MNC;
private TextView PSC;
private TextView LAC;
private TextView IP;

/**
* Member variables for UI of the location data
**/
private TextView Latitude;
private TextView Longitude;
private TextView Altitude;
private TextView Accuracy;
private TextView Bearing;
private TextView SpeedOfMovement;

/**
* Used to start and stop and data collection in listeners
**/
private boolean isRunning = false;

/**
* Class to access telephony services of the device
**/
TelephonyManager Tel;

/**
* Can be used to filter log messages
**/
private static final String TAG = "nanda_wnp_debug";


@SuppressLint("SetTextI18n")
public void onCreate(Bundle savedInstanceState) {

    /***Write headers for the CSV file***/
    WriteHeadersToFile();

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_tel);

    /***ON BUTTON CLICK -- MAIN MENU***/
    Button Main = (Button) findViewById(R.id.Main_Menu);
    Main.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent Main_Intent = new Intent(Telephony_Manager_screen.this, MainActivity.class);
            startActivity(Main_Intent);
        }
    });

    /***ON BUTTON CLICK --  SAVE DATA***/
    Button SaveButton = (Button) findViewById(R.id.saveButton);
    SaveButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            saveData();
        }
    });

    /***FIND THE VIEW IDENTIFIED BY ID ATTRIBUTE***/
    Signal_Strength = (TextView) findViewById(R.id.Text_SS);
    Signal_Strength_dbm = (TextView) findViewById(R.id.Text_SS_dbm);
    UTRAN_CID = (TextView) findViewById(R.id.Text_UTRAN_CID);
    CID = (TextView) findViewById(R.id.Text_CID);
    RNC_ID = (TextView) findViewById(R.id.Text_RNC_ID);
    MCC = (TextView) findViewById(R.id.Text_MCC);
    MNC = (TextView) findViewById(R.id.Text_MNC);
    PSC = (TextView) findViewById(R.id.Text_PSC);
    LAC = (TextView) findViewById(R.id.Text_LAC);
    IP = (TextView) findViewById(R.id.Text_IP);

    /***ACCESS TELEPHONY SERVICES***/
    MyPhoneStateListener MyListener = new MyPhoneStateListener();
    Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

    /***FIND THE VIEW IDENTIFIED BY ID ATTRIBUTE***/
    Latitude = (TextView) findViewById(R.id.Text_Latitude);
    Longitude = (TextView) findViewById(R.id.Text_Longitude);
    Altitude = (TextView) findViewById(R.id.Text_Altitude);
    Accuracy = (TextView) findViewById(R.id.Text_Accuracy);
    Bearing = (TextView) findViewById(R.id.Text_bearing);
    SpeedOfMovement = (TextView) findViewById(R.id.Text_Speed);

    /***ACCESS TELEPHONY SERVICES***/
    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    MyLocationListener MyLocListener = new MyLocationListener();

    Criteria criteria = new Criteria();
    String provider = locationManager.getBestProvider(criteria, false);
    Location location = locationManager.getLastKnownLocation(provider);

    if (location != null) {
        MyLocListener.onLocationChanged(location);
    } else {

        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);

        Latitude.setText("Location not available");
        Longitude.setText("Location not available");
        Altitude.setText("Location not available");
        Accuracy.setText("Location not available");
        Bearing.setText("Location not available");

    }
    locationManager.requestLocationUpdates(provider, 200, 1, MyLocListener);
}

/**
* Resume data collection
**/
@Override
protected void onResume() {
    super.onResume();
    isRunning = true;
}

/**
* Pause data collection
**/
@Override
protected void onPause() {
    super.onPause();
    isRunning = false;
}

private class MyPhoneStateListener extends PhoneStateListener {
    @SuppressLint("SetTextI18n")
    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {

        /***App not in foreground, so don't do anything***/
        if (!isRunning)
            return;

        /***GET SIGNAL STRENGTH***/
        SS_val = signalStrength.getGsmSignalStrength();
        Signal_Strength.setText(String.valueOf(SS_val));

        /***GET SIGNAL STRENGTH IN dbm***/
        if (SS_val != 99) {
            SS_in_dbm_val = -113 + 2 * SS_val;
            Signal_Strength_dbm.setText(String.valueOf(SS_in_dbm_val));
        } else {
            Signal_Strength_dbm.setText("No Signal");
        }

        /***GET CID AND LAC***/
        GsmCellLocation cellLocation = (GsmCellLocation) Tel.getCellLocation();
        CID_val = cellLocation.getCid();
        /***If UMTS network, then extract UTRAN CID and RNC ID***/
        if (CID_val > 0xffff) {
            UTRAN_CID_val = CID_val;
            UTRAN_CID.setText(String.valueOf(UTRAN_CID_val));
            RNC_ID_val = CID_val >> 16;
            RNC_ID.setText(String.valueOf(RNC_ID_val));
            CID_val = CID_val & 0xffff;
        }
        LAC_val = cellLocation.getLac();
        CID.setText(String.valueOf(CID_val));
        LAC.setText(String.valueOf(LAC_val));

        String networkOperator = Tel.getNetworkOperator();
        MCC_val = Integer.parseInt(networkOperator.substring(0, 3));
        MNC_val = Integer.parseInt(networkOperator.substring(3));
        PSC_val = cellLocation.getPsc();
        MCC.setText(String.valueOf(MCC_val));
        MNC.setText(String.valueOf(MNC_val));
        PSC.setText(String.valueOf(PSC_val));

        /***GET IP ADDRESS***/
        IP_val = getLocalIpAddress();
        IP.setText(IP_val);

        /***Write current data to file***/
        event_val = "Signal Strength Changed";
        WriteToFile();
    }
}

public String getLocalIpAddress() {
    try {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface net_interface = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = net_interface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                String ipv4 = inetAddress.getHostAddress();
                // for getting IPV4 format
                if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4)) {
                    return ipv4;
                }
            }
        }
    } catch (Exception ignored) {
    }
    return null;
}

private class MyLocationListener implements LocationListener {
    @Override
    public void onLocationChanged(Location location) {

        /***App not in foreground, so don't do anything***/
        if (!isRunning)
            return;

        lat_val = location.getLatitude();
        log_val = location.getLongitude();
        alt_val = location.getAltitude();
        accuracy_val = location.getAccuracy();
        bearing_val = location.getBearing();
        speed_val = location.getSpeed();
        speed_val = (speed_val * 2.23694);

        Latitude.setText(String.valueOf(lat_val));
        Longitude.setText(String.valueOf(log_val));
        Altitude.setText(String.valueOf(alt_val));
        Accuracy.setText(String.valueOf(accuracy_val));
        Bearing.setText(String.valueOf(bearing_val));
        SpeedOfMovement.setText(String.valueOf(speed_val));

        /***Write current data to file***/
        event_val = "Location Changed";
        WriteToFile();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}

public void saveData() {
    View rootView = findViewById(android.R.id.content).getRootView();
    rootView.setDrawingCacheEnabled(true);

    try {
        /***Create folder, if not present***/
        File path = new File(Environment.getExternalStorageDirectory()
            + File.separator + "_nanda_wnp");
        path.mkdirs();
        /***Name the file using current time and dataA***/
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        FileOutputStream fos = new FileOutputStream(
            new File(path + File.separator + df.format(new Date()) + ".png"));
        rootView.getDrawingCache().compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();
    } catch (IOException e) {
        Log.e("saveData", e.getMessage(), e);
    }

    Toast.makeText(getApplicationContext(), "Data has been saved",
        Toast.LENGTH_SHORT).show();

    rootView.destroyDrawingCache();
}

/**
* If csv file is not present, create it and write the column headers
*/
public void WriteHeadersToFile() {
    try {
        File path = new File(Environment.getExternalStorageDirectory()
            + File.separator + "_nanda_wnp");
        path.mkdirs();

        File f = new File(path + File.separator + "nanda_wnp.csv");
        if (f.exists())
            return;
        FileOutputStream fos = new FileOutputStream(f, true);

        fos.write("Event,".getBytes());
        fos.write("Signal Strength,".getBytes());
        fos.write("Signal Strength (dbm),".getBytes());
        fos.write("UTRAN CID,".getBytes());
        fos.write("Cell ID,".getBytes());
        fos.write("RNC ID,".getBytes());
        fos.write("LAC,".getBytes());
        fos.write("MCC,".getBytes());
        fos.write("MNC,".getBytes());
        fos.write("PSC,".getBytes());
        fos.write("Latitude,".getBytes());
        fos.write("Longitude,".getBytes());
        fos.write("Altitude,".getBytes());
        fos.write("Bearing,".getBytes());
        fos.write("Accuracy,".getBytes());
        fos.write("IP,".getBytes());
        fos.write("Speed (mph)".getBytes());
        fos.write(System.getProperty("line.separator").getBytes());
        fos.flush();
        fos.close();
    } catch (IOException e) {
        Log.e("WriteToFile", e.getMessage(), e);
    }
}

/**
* Write telephony and location data to the csv file
*/
public void WriteToFile() {
    try {
        File path = new File(Environment.getExternalStorageDirectory()
            + File.separator + "_nanda_wnp");
        path.mkdirs();
        FileOutputStream fos = new FileOutputStream(
            new File(path + File.separator + "nanda_wnp.csv"), true);

        fos.write(event_val.getBytes());
        fos.write(",".getBytes());
        fos.write(Integer.toString(SS_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Integer.toString(SS_in_dbm_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Integer.toString(UTRAN_CID_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Integer.toString(CID_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Integer.toString(RNC_ID_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Integer.toString(LAC_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Integer.toString(MCC_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Integer.toString(MNC_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Integer.toString(PSC_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Double.toString(lat_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Double.toString(log_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Double.toString(alt_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Double.toString(bearing_val).getBytes());
        fos.write(",".getBytes());
        fos.write(Double.toString(accuracy_val).getBytes());
        fos.write(",".getBytes());
        fos.write(IP_val.getBytes());
        fos.write(",".getBytes());
        fos.write(Double.toString(speed_val).getBytes());
        fos.write(System.getProperty("line.separator").getBytes());
        fos.flush();
        fos.close();
    } catch (IOException e) {
        Log.e("WriteToFile", e.getMessage(), e);
    }
}

}
