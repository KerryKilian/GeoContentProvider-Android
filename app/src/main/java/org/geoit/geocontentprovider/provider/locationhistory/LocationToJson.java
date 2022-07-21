package org.geoit.geocontentprovider.provider.locationhistory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Kilian Aaron Brinkner
 * This class forms data from the location history database into a json object
 */
public class LocationToJson {
    private Context context;
    private static String TAG = "mLog";
    private String selectionArgs = "";
    String afterDateTime = "";
    String beforeDateTime = "";
    JSONObject json = new JSONObject();
    JSONArray array = new JSONArray();
    String afterDate;
    String beforeDate;
    String afterTime;
    String beforeTime;

    public void log(String string) {
        Log.d("mLog", string);
    }

    public String getJson() {
        return json.toString();
    }

    @SuppressLint("Range")
    public LocationToJson(Context context, Bundle bundle) {
        this.context = context;
        afterDate = bundle.getString("afterDate");
        beforeDate = bundle.getString("beforeDate");
        afterTime = bundle.getString("afterTime");
        beforeTime = bundle.getString("beforeTime");
//        String id = null;
//        if (uriMatcher.match(uri) == LOCATION_ID) {
//            //Query is for one single location. Get the ID from the URI.
//            id = uri.getPathSegments().get(1);
//        }

        parseDateTimeQuery();
        parseIntoJson(searchInDatabase());

    }

    private void parseDateTimeQuery() {
        if (afterDate != null) {
            afterDateTime += afterDate;
            if (afterTime != null) {
                afterDateTime += " " + afterTime;
            }
        } else { // afterDate is null
            if (afterTime != null) {
                afterDateTime += afterTime;
            }
        }

        if (beforeDate != null) {
            beforeDateTime += beforeDate;
            if (beforeTime != null) {
                beforeDateTime += " " + beforeTime;
            }
        } else { // beforeDate is null
            if (beforeTime != null) {
                beforeDateTime += beforeTime;
            }
        }

        if (afterDateTime.equals("") == false) {
            selectionArgs += "TIMESTAMP > datetime('" + afterDateTime + "')";
            if (beforeDateTime.equals("") == false) {
                selectionArgs += " AND TIMESTAMP < datetime('" + beforeDateTime + "')";
            }
        } else { // no afterDateTime required
            if (beforeDateTime.equals("") == false) {
                selectionArgs += "TIMESTAMP < datetime('" + beforeDateTime + "')";
            }
        }

        Log.d(TAG, selectionArgs);
    }

    @SuppressLint("Range")
    private Cursor searchInDatabase() {
        PrivateLocationDatabase privateLocationDatabase = new PrivateLocationDatabase(context);
        Cursor cursor = privateLocationDatabase.getLocation(null /**id*/, null,
                selectionArgs, null, null);

        return cursor;
    }

    @SuppressLint("Range")
    private void parseIntoJson(Cursor cursor) {


        try {
            json.put("afterDate", afterDate);
            json.put("afterTime", afterTime);
            json.put("beforeDate", beforeDate);
            json.put("beforetime", beforeTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                Log.d(TAG,"id: " +  cursor.getString(cursor.getColumnIndex("_id")) + "  "+
                        "timestamp: " +  cursor.getString(cursor.getColumnIndex("timestamp")) + "  "+
                        "lat: " + cursor.getString(cursor.getColumnIndex("lat")) + "  "+
                        "lon: " + cursor.getString(cursor.getColumnIndex("lon")));

                JSONObject point = new JSONObject();
                try {
                    point.put("timestamp", cursor.getString(cursor.getColumnIndex("timestamp")));
                    point.put("lat", cursor.getString(cursor.getColumnIndex("lat")));
                    point.put("lon", cursor.getString(cursor.getColumnIndex("lon")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                array.put(point);

                cursor.moveToNext();
            }
        }
        else {
            Log.d(TAG, "No Records Found");
        }

        try {
            json.put("elements", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
