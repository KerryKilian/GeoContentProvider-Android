package org.geoit.geocontentprovider.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.geoit.geocontentprovider.R;
import org.geoit.geocontentprovider.provider.feature.get.PbfToGeoJson;
import org.geoit.geocontentprovider.provider.feature.get.PbfToJson;
import org.geoit.geocontentprovider.provider.feature.get.PbfToOsm;
import org.geoit.geocontentprovider.provider.feature.update.JsonToPbf;
import org.geoit.geocontentprovider.provider.geocoding.ReverseGeoCoder;
import org.geoit.geocontentprovider.provider.locationhistory.LocationToJson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Kilian Aaron Brinkner
 * Queries to the provider target the method "call" in this class
 */
public class ProviderApi extends ContentProvider {

    private static final String PROVIDER_NAME = "org.geoit.geocontentprovider";
    private static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "");
    private static final int LOCATION = 1;
    private static final int LOCATION_ID = 2;
    private static final UriMatcher uriMatcher = getUriMatcher();

    private static UriMatcher getUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "service", LOCATION);
        uriMatcher.addURI(PROVIDER_NAME, "service/#", LOCATION_ID);
        return uriMatcher;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case LOCATION:
                return "vnd.android.cursor.dir/vnd.org.geoit.geocontentprovider.provider.service";
            case LOCATION_ID:
                return "vnd.android.cursor.item/vnd.org.geoit.geocontentprovider.provider.service";

        }
        return "";
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        return true;
    }

    /**
     * helper method for logging
     * @param string
     */
    private void log(String string) {
        Log.d("mLog", string);
    }

    /**
     * Interface. Other apps call this "call"-method and give the parameters.
     * This method calculates what data the client app needs and puts it into a response.
     * @param uriString
     * @param method
     * @param arg
     * @param bundle
     * @return
     */
    @Override
    public Bundle call(String uriString, String method, String arg, Bundle bundle) {
        log("Content Provider was called now");

        String request = bundle.getString("request");
        String format = bundle.getString("format");
        String typename = bundle.getString("typename");
        String id = bundle.getString("id");
        String callingApp = getCallingPackage();
        log(callingApp);

        switch (request) {
            case "getFeature":
                switch (typename) {
                    case "public":
                        log("request: getFeature / typename: public");

                        // Specify inputstream
                        InputStream input = getContext().getResources().openRawResource(R.raw.berlin_latest);
                        switch (format) {
                            case "json":
                                PbfToJson pbfToJson = new PbfToJson(getContext(), bundle, input);
                                bundle.putString("response", pbfToJson.getResult());
                                bundle.putInt("status", 200);
                                break;
                            case "osm":
                                PbfToOsm pbfToOsm = new PbfToOsm(getContext(), bundle, input);
                                bundle.putString("response", pbfToOsm.getResult());
                                bundle.putInt("status", 200);
                                break;
                            case "geojson":
                                PbfToGeoJson pbfToGeoJson = new PbfToGeoJson(getContext(), bundle, input);
                                bundle.putString("response", pbfToGeoJson.getResult());
                                bundle.putInt("status", 200);
                                break;
                            default:
                                bundle.putString("response", "The format '" + format + "' is not implemented yet. " +
                                        "Please use 'json', 'osm' or 'geojson'.");
                                bundle.putInt("status", 501);
                                break;
                        }
                        log("response " + bundle.getString("response"));
                        bundle.putInt("status", 200);
                        break;
                    case "private":
                        log("request: getFeature / typename: private");

                        InputStream inputStream = null;
                        PbfToJson pbfToJson = null;
                        try {
                            inputStream = createInputStreamPrivate(id);
                            pbfToJson = new PbfToJson(getContext(), bundle, inputStream);
                        } catch (NullPointerException e) {
                            bundle.putString("response", "ID '" + id + "' does not exist. You must insert data with this ID first, " +
                                    "then you can ask for individual data. Error Code: 404");
                            bundle.putInt("status", 404);
                            break;
                        }

                        bundle.putString("response", pbfToJson.getResult());
                        bundle.putInt("status", 200);
                        break;
                    default:
                        bundle.putString("response", "The typename '" + typename + "' does not exist for request 'getFeature'. " +
                                "Please use 'public' or 'private'.");
                        bundle.putInt("status", 404);
                }
                break;
            case "getLocationHistory":
                LocationToJson locationParser = new LocationToJson(getContext(), bundle);
                bundle.putString("response", locationParser.getJson());
                bundle.putInt("status", 200);
                break;
            case "updateFeature":
                log("request: updateFeature");
                JsonToPbf jsonToPbf = new JsonToPbf(bundle, getContext());

                String outputString = "";
                try {
                    int bytes = createInputStreamPrivate(id).available();
                    outputString = "Successfully inserted data in file with " +
                            bytes + " bytes.";
                    if (bytes < 100) {
                    outputString += "Your file is very small. Either you inserted only small data amounts or something went wrong";
                    }
                    if (bytes == 0) {
                        outputString = "Data could not be inserted. " +
                                "Try again with same parameters or check your parameters";
                        bundle.putString("response", outputString);
                        bundle.putInt("status", 500);
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bundle.putString("response", outputString);
                bundle.putInt("status", 200);
                break;
            default:
                bundle.putString("response", "The request '" + request + "' does not exists. Please use " +
                        "'getFeature', 'updateFeature' or 'getUserLocationHistory'." +
                        "Error code: 404");
                bundle.putInt("status", 404);
                break;

            case "geocoding":
                log("request: getGeocoding");
                InputStream inputStream = getContext().getResources().openRawResource(R.raw.de_txt);
                ReverseGeoCoder reverseGeoCode = null;

                try {
                    reverseGeoCode = new ReverseGeoCoder(
                            inputStream, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                bundle.putString("response", reverseGeoCode.nearestPlace(
                        bundle.getDouble("lat"),
                        bundle.getDouble("lon")).name);
                bundle.putInt("status", 200);
                break;

        }
            log(bundle.getString("response"));
        return bundle;
    }

    /**
     * creates an InputStream-Object for the private feature request
     * @param id
     * @return
     */
    private InputStream createInputStreamPrivate(String id) {
        // Specify inputstream
        ContextWrapper cw = new ContextWrapper(getContext());
        File directory = cw.getDir(id, Context.MODE_PRIVATE);


        InputStream input = null;
        try {
            input = new FileInputStream(directory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return input;
    }

    /**
     * must be implemented because of super class ContentProvider
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    /**
     * must be implemented because of super class ContentProvider
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    /**
     * must be implemented because of super class ContentProvider
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    /**
     * must be implemented because of super class ContentProvider
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


}
