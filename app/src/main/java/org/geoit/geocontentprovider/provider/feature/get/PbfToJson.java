package org.geoit.geocontentprovider.provider.feature.get;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.wolt.osm.parallelpbf.ParallelBinaryParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geoit.geocontentprovider.R;

/**
 * @author Kilian Aaron Brinkner
 * This class parses data from the pbf file into a json object.
 * It is the main class for parseing. Every other format (osm, geojson, gml) refers to this response.
 * So if the calculation method shall be changed, it can be done here and the calculation changes
 * for every format.
 * I am using the Parallel OSM PBF Parser from Denis Chaplygin and Scott Crosby
 */
public class PbfToJson {
    private static final String TAG = "GeoProvider";
    private static final int numberOfThreads = 10;
    private String type;
    private List<String[]> keysAndValues = new ArrayList<>();
    private Context context;
    private double minLat;
    private double minLon;
    private double maxLat;
    private double maxLon;
    private InputStream input;
    private JSONObject response = new JSONObject();
    private JSONArray elements = new JSONArray();
    private List<JSONArray> refNodesList = new ArrayList<>(); // refNodes for one way in one jsonarray

    /**
     * gets called from ProviderApi class
     * @param context
     * @param bundle
     */
    public PbfToJson(Context context, Bundle bundle, InputStream input) {
        this.context = context;
        this.input = input;
        parseBundle(bundle);
        parsePbf();
    }

    /**
     * parses requested bundle into local attributes
     * @param bundle
     */
    public void parseBundle(Bundle bundle) {
        this.minLat = bundle.getDouble("minLat");
        this.minLon = bundle.getDouble("minLon");
        this.maxLat = bundle.getDouble("maxLat");
        this.maxLon = bundle.getDouble("maxLon");
        this.type = bundle.getString("type");

        // Get all tags
        Set<String> keys = bundle.keySet();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            keys.forEach(key -> {
                log(key);
                if (!key.equals("minLat") && !key.equals("minLon") && !key.equals("maxLat") && !key.equals("maxLon") && !key.equals("type")) {
                    keysAndValues.add(new String[] {key, bundle.getString(key)});

                    try {
                        response.put(key, bundle.getString(key));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        try {
            response.put("minLat", minLat);
            response.put("minLon", minLon);
            response.put("maxLat", maxLat);
            response.put("maxLon", maxLon);
            response.put("type", type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * inserts content values into the final json response object
     * @param contentValues
     */
    private void insertContentValuesIntoJson(ContentValues contentValues, List<Long> refNodes) {
        Set<String> keySet = contentValues.keySet();
        JSONObject newObject = new JSONObject();
        JSONObject tags = new JSONObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            keySet.forEach(key -> {
                try {
                    log("key: " + key);
                    if (key.equals("id") || key.equals("lat") || key.equals("lon") || key.equals("type")) {
                        newObject.put(key, contentValues.get(key));
                    } else if (key.equals("nodes")) {
                        JSONArray nodeIds = new JSONArray();
                        for (Long refNode : refNodes) {
                            nodeIds.put(refNode);
                        }
                        newObject.put("nodes", nodeIds);
                    }
                    else {
                        tags.put(key, contentValues.get(key));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            });
        }

        if (tags != null) {
            try {
                newObject.put("tags", tags);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        elements.put(newObject);
    }

    private void log(String string) {
        Log.d(TAG, string);
    }

    /**
     * Call this method if you want to get the final JSONObject in json format.
     * Usage:
     * PbfToJson parser = new PbfToJson;
     * parser.getJsonResult();
     * @return
     */
    public JSONObject getJsonResult() {
        return this.response;
    }

    /**
     * Call this method if you want to get the final String in json format.
     * Usage:
     * PbfToJson parser = new PbfToJson;
     * parser.getResult();
     * @return
     */
    public String getResult() {
        return this.response.toString();
    }

    private void insertIntoJson() {
        try {
            response.put("elements", elements);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parsePbf() {
        if (type.equals("node")) {
            parseNodes();
        } else if (type.equals("way")) {
            parseWays();
        } else if (type.equals("relation")) {
            // must be implemented. Too difficult for me
            //parseRelations();
        } else {
            Log.e(TAG, type + " cannot be resolved. It should be either node, way or relation");
        }
    }

    private void parseNodes() {
        log("Beginning of Node Parsing");

        new ParallelBinaryParser(input, numberOfThreads)
                .onComplete(() -> {
                    // insert all elements from jsonArray into response jsonObject
                    insertIntoJson();
                    log("End of Node Parsing");
                })
                .onNode(node -> {
                    Map<String, String> tags = node.getTags();
                    ContentValues contentValues = new ContentValues();

                    // checks for all requested nodes. BUT: more tags mean OR , not AND
                    for (String[] kv : keysAndValues) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            tags.forEach((key, value) -> {
                                if (key.equals(kv[0])) {
                                    if (value.equals(kv[1])) {
                                        log("tag contains value: " + kv[1]);

                                        double lat = node.getLat();
                                        double lon = node.getLon();

                                        if (isInBbox(lat, lon) == true) {
                                            contentValues.put("type", type);
                                            contentValues.put("id", node.getId());
                                            contentValues.put("lat", lat);
                                            contentValues.put("lon", lon);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                tags.forEach((k, v) -> contentValues.put(k, v));
                                            }
                                            insertContentValuesIntoJson(contentValues, null);
                                        }
                                    }
                                }
                            });
                        }
                    }

                }).parse();
    }


    private boolean isInBbox(double lat, double lon) {
        if (lat > minLat && lat < maxLat) {
            if (lon > minLon && lon < maxLon) {
                return true;
            }
        }
        return false;
    }


    private void parseWays() {
        List<List<Long>> refNodes = new ArrayList<>();
        List<Boolean> wayInBbox = new ArrayList<>();
        List<ContentValues> wayContentValues = new ArrayList<>();

        new ParallelBinaryParser(input, numberOfThreads)
                .onWay(way -> {
                    Map<String, String> tags = way.getTags();

                    // If tag belongs to searched tag
                    for (String[] kv : keysAndValues) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            tags.forEach((key, value) -> {
                                if (key.equals(kv[0])) {
                                    if (value.equals(kv[1])) {
                                        // If we are here, it means, the way has got the searched key and values

                                        log("proper way found");
                                        ContentValues contentValues = new ContentValues();

                                        // client sees what typed he asked for
                                        contentValues.put("type", "way");
                                        // every way has got an id
                                        contentValues.put("id", way.getId());
                                        // node ids of way added to list (later the list will be checked for bbox)
                                        refNodes.add(way.getNodes());

                                        // Insert node ids into content values column "nodes"
                                        JSONArray array = new JSONArray();
                                        for (Long node : way.getNodes()) {
                                            array.put(node);
                                        }
                                        contentValues.put("nodes", array.toString());

                                        // insert keys
                                        tags.forEach((k, v) -> contentValues.put(k, v));

                                        wayInBbox.add(false);
                                        // these contentValues of way will be saved in list
                                        wayContentValues.add(contentValues);
                                        log("way added to list");
                                    }
                                }
                            });
                        }
                    }
                })
                .onComplete(new Runnable() {
                    @Override
                    public void run() {
                        // on Complete search for nodes from the ways
                        searchNodesFromWays(refNodes, wayInBbox, wayContentValues);
                    }
                })
                .parse();
    }

    /**
     * Searches the nodes from the ways which have the proper tags.
     * Creates new ParallelBinaryParser to look for the nodes and checks wether the nodes are in bounding box
     */
    private void searchNodesFromWays(List<List<Long>> refNWR, List<Boolean> nWRInBbox, List<ContentValues> nWRContentValues) {
        log("now searching ids");
        int refNWRSize = refNWR.size();

        // new InputStream to be safe that ParallelBinaryParser begins properly
        InputStream input2 = context.getResources().openRawResource(R.raw.berlin_latest);
        new ParallelBinaryParser(input2, numberOfThreads)
                .onNode(node -> {



                    // if node id is equal to any id saved in the lists
                    for (int n = 0; n < refNWRSize; n++) {
                        for (int a = 0; a < refNWR.get(n).size(); a++) {
                            if (refNWR.get(n).get(a) == node.getId()) {
                                log("ids identical found");
                                int finalN = n;
                                new Thread() {
                                    public void run() {
// if node in bbox
                                        if (isInBbox(node.getLat(), node.getLon())) {
                                            // insert node into database
                                            ContentValues contentValues = new ContentValues();
                                            contentValues.put("type", "node");
                                            contentValues.put("id", node.getId());
                                            contentValues.put("lat", node.getLat());
                                            contentValues.put("lon", node.getLon());
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                node.getTags().forEach((k, v) -> contentValues.put(k, v));
                                            }
                                            insertContentValuesIntoJson(contentValues, null);


                                            // way belonging to node gets true because at least one point of way in bbox
                                            // (later this way will be added to database)
                                            nWRInBbox.set(finalN, true);
                                        }
                                    }
                                }.start();


                            }

                        }
                    }
                })
                .parse();

        // every way who has got at least one point in bbox will be added to database
        for (int n = 0; n < nWRInBbox.size(); n++) {
            if (nWRInBbox.get(n) == true) {
                insertContentValuesIntoJson(nWRContentValues.get(n), refNWR.get(n));
            }
        }

        // insert all elements from jsonArray into response jsonObject
        insertIntoJson();
        log("way / relation parsing ended");
    }

}
