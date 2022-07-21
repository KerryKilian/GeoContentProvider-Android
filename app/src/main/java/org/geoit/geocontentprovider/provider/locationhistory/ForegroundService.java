package org.geoit.geocontentprovider.provider.locationhistory;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.geoit.geocontentprovider.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ForegroundService extends Service {


    private final IBinder mBinder = new MyBinder();
    private static final String CHANNEL_ID = "2";
    private static final String PROVIDER_NAME = "de.geoprovider.provider";
    private static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/privatelocation"); // braucht man nicht
    public static final int INTERVAL = 1200000;//1200000;5000;1800000
    public static final int FASTEST_INTERVAL = 1200000;//1200000;5000;1800000


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        buildNotification();
        requestLocationUpdates();
    }
    private void buildNotification() {
        String stop = "stop";
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_IMMUTABLE);
        // Create the persistent notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Location tracking is working")
                .setOngoing(true)
                .setContentIntent(broadcastIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.setDescription("Location tracking is working");
            channel.setSound(null, null);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
        startForeground(1, builder.build());
    }
    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();
        request.setInterval(INTERVAL);
        request.setFastestInterval(FASTEST_INTERVAL);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    String location = "Latitude : " + locationResult.getLastLocation().getLatitude() +
                            "\nLongitude : " + locationResult.getLastLocation().getLongitude();
                    Toast.makeText(ForegroundService.this, location, Toast.LENGTH_SHORT).show();
                    Log.d("mLog", location);

                    // Schreibe Location in ContentProvider
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
                    String currentDateandTime = sdf.format(new Date());
                    Log.i("Date",currentDateandTime);

                    ContentValues contentValues = new ContentValues();
                    //contentValues.put("timestamp", sdf.format(new Date()));
                    contentValues.put("lat" , locationResult.getLastLocation().getLatitude());
                    contentValues.put("lon", locationResult.getLastLocation().getLongitude());
                    //Uri uri = getContentResolver().insert(CONTENT_URI, contentValues);
                    PrivateLocationDatabase privateLocationDatabase = new PrivateLocationDatabase(getApplicationContext());
                    privateLocationDatabase.addNewLocation(contentValues);
                }
            }, null);
        } else {
            stopSelf();
        }
    }
    public class MyBinder extends Binder {
        public ForegroundService getService() {
            return ForegroundService.this;
        }
    }
}