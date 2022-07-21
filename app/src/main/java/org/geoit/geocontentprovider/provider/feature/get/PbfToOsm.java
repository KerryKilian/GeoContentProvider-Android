package org.geoit.geocontentprovider.provider.feature.get;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * @author Kilian Aaron Brinkner
 * This class parses data from the pbf file into a string object with osm xml structure
 */
public class PbfToOsm {
    JSONObject json;
    String result;
    double minLat;
    double minLon;
    double maxLat;
    double maxLon;



    public PbfToOsm(Context context, Bundle bundle, InputStream inputStream) {
        PbfToJson pbfToJson = new PbfToJson(context, bundle, inputStream);
        json = pbfToJson.getJsonResult();

        minLat = bundle.getDouble("minLat");
        minLon = bundle.getDouble("minLon");
        maxLat = bundle.getDouble("maxLat");
        maxLon = bundle.getDouble("maxLon");


        OsmXmlCreator osmXmlCreator = new OsmXmlCreator(minLat, minLon, maxLat, maxLon);


        try {
            JSONArray elements = json.getJSONArray("elements");
            for (int n = 0; n < elements.length(); n++) {
                JSONObject item = elements.getJSONObject(n);
                String type = item.getString("type");

                if (type.equals("node")) {
                    log("node found");
                    osmXmlCreator.insertJsonNode(item);
                } else if (type.equals("way")) {
                    osmXmlCreator.insertJsonWay(item);
                }

                log("neither node or way");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        result = osmXmlCreator.getOsmXmlResult();
    }

    /**
     * Get Result
     * @return
     */
    public String getResult() {
        return result;
    }

    /**
     * helper method for logging
     * @param string
     */
    private void log(String string) {
        Log.d("mLog", string);
    }
}
