package org.geoit.geocontentprovider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.geoit.geocontentprovider.provider.locationhistory.ForegroundService;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "mLog";
    String type;
    List<String[]> keysAndValues;
    private static final String PROVIDER_NAME = "org.geoit.geocontentprovider";
    private static Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/feature");

    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Button beginLocationHistoryBtn = findViewById(R.id.begin_location_history);
        beginLocationHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Standortverlauf");
                builder.setMessage("Vielen Dank, dass du den Standortverlauf aktivieren möchtest. " +
                        "Damit dieser jedoch permanent funktioniert und wir dir ein optimales Erlebnis bieten können," +
                        "musst du in den App-Einstellungen den dauerhaften Zugriff auf den Standort gewähren." +
                        "Ohne diesen Zugriff können andere Apps möglicherweise nicht richtig arbeiten.\n" +
                        "Gehe zu Einstellungen \n-> Apps \n-> GCP Service \n-> Berechtigungen \n-> Standort \n-> Permamenter Zugriff erlauben");
                builder.setPositiveButton("App-Einstellungen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        openSettings();
                    }
                });
                builder.setNegativeButton("Zurück", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    enableLocationSettings();
                } else {
                    requestAppPermissions();
                }
            }
        });

        keysAndValues = new ArrayList<>();

        String[] selectionArgsMitte = new String[]{"way", "52.511896", "13.390900", "52.527381", "13.420148", "name=Alexanderplatz"};
        String[] selectionArgsBergmannKiez = new String[]{"way", "52.484252", "13.384493", "52.497678", "13.410450", "name=Alexanderplatz"};
        String[] selectionArgsMitteBus = new String[]{"node", "52.484252", "13.384493", "52.497678", "13.410450", "highway=bus_stop"};
        String[] selectionArgsBergmannKiezSchule = new String[]{"way",  "52.484252", "13.384493", "52.497678", "13.410450", "amenity=school"};

        // For routing request
        String [] routingBerlin = new String[]{"52.492437", "13.337929", "52.512587", "13.388456"};
        String [] routingBremen = new String[]{"53.061553", "8.801264", "53.041387", "8.855077"};
        String [] routingLiechtenstein = new String[]{"47.134872", "9.518747", "47.162160", "9.510127"};

        //searchFor(selectionArgsMitte);

    }

    private void requestAppPermissions() {
        Dexter.withActivity(MainActivity.this)
                .withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            // do you work now
                            //interact.downloadImage(array);
                            startService(new Intent(MainActivity.this, ForegroundService.class));
                        }
                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                            showSettingsDialog();
                            //finish();
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();
    }


    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }


    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);

    }


    protected void enableLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(3000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        LocationServices
                .getSettingsClient(this)
                .checkLocationSettings(builder.build())
                .addOnSuccessListener(this, (LocationSettingsResponse response) -> {
                    // startUpdatingLocation(...);
                })
                .addOnFailureListener(this, ex -> {
                    if (ex instanceof ResolvableApiException) {
                        // Location settings are NOT satisfied,  but this can be fixed  by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),  and check the response in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) ex;
                            resolvable.startResolutionForResult(this, 123);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

}