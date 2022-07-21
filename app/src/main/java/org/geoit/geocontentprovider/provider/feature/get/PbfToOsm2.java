package org.geoit.geocontentprovider.provider.feature.get;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.wolt.osm.parallelpbf.ParallelBinaryParser;
import com.wolt.osm.parallelpbf.entity.Way;

import org.geoit.geocontentprovider.R;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Kilian Aaron Brinkner
 * Deprecated!
 * This class parses data from the pbf file into a string object with osm xml structure.
 * Due to programmatical structure changes in the format parsing (every format shall take the result from json object),
 * this classes should not be used. But it can be that there were some implementations that I did not see,
 * so for safety reasons, this class will be available here
 */
public class PbfToOsm2 {
    private static final String TAG = "GeoProvider";
    private static final int numberOfThreads = 24;
    private String type;
    private List<String[]> keysAndValues = new ArrayList<>();
    private Context context;
    private double minLat;
    private double minLon;
    private double maxLat;
    private double maxLon;
    private InputStream input;
    private String response;
    private StringBuilder builder = new StringBuilder();
    private OsmXmlCreator creator;

    public PbfToOsm2(Context context, Bundle bundle, InputStream input) {
        this.context = context;
        // read pbf file
        this.input = input;
        parseBundle(bundle);
        parsePbf();
    }

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
                }
            });
        }

        creator = new OsmXmlCreator(minLat, minLon, maxLat, maxLon);
    }

    public void log(String string) {
        Log.d(TAG, string);
    }

    public String getResult() {
        return creator.getOsmXmlResult();
    }

    int i = 1;
    public void parsePbf() {
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

    public void parseNodes() {
        new ParallelBinaryParser(input, numberOfThreads) //  noPartitions, myShard
                .onComplete(() -> {
                    log("node parsing ended");
                })
                .onNode(node -> {
                    Map<String, String> tags = node.getTags();

                    // checks for all requested nodes. BUT: more tags mean OR , not AND
                    for (String[] kv : keysAndValues) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            tags.forEach((key, value) -> {
                                if (key.equals(kv[0])) {
                                    if (value.equals(kv[1])) {
                                        //log("tag contains value: " + kv[1]);

                                        if (isInBbox(node.getLat(), node.getLon()) == true) {
                                            log("node inside bbox");
                                            creator.insertNode(node);
                                            log("creator: " + creator.toString());
                                        }
                                    }
                                }
                            });
                        }
                    }
                }).parse();

    }

    public boolean isInBbox(double lat, double lon) {
        if (lat > minLat && lat < maxLat) {
            if (lon > minLon && lon < maxLon) {
                return true;
            }
        }
        return false;
    }


    public void parseWays() {
        List<List<Long>> refNodes = new ArrayList<>();
        List<Boolean> wayInBbox = new ArrayList<>();
        List<Way> wayContentValues = new ArrayList<>(); // if relation search is implemented: OsmEntity instead of Way

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

                                        // node ids of way added to list (later the list will be checked for bbox)
                                        refNodes.add(way.getNodes());

                                        wayInBbox.add(false);
                                        // these contentValues of way will be saved in list
                                        wayContentValues.add(way);
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
    public void searchNodesFromWays(List<List<Long>> refNWR, List<Boolean> nWRInBbox, List<Way>/*osmEntity*/ nWRContentValues) {
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
                                            creator.insertNode(node);


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

        // every way who has got at least one point in bbox will be added to xml
        for (int n = 0; n < nWRInBbox.size(); n++) {
            if (nWRInBbox.get(n) == true) {
                creator.insertWay(nWRContentValues.get(n));
            }
        }

        log("way parsing ended");
    }

}
