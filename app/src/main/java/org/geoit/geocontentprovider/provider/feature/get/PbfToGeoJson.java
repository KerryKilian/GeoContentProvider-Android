package org.geoit.geocontentprovider.provider.feature.get;

import android.content.Context;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kilian Aaron Brinkner
 * This class parses data from the pbf file into a geojson object.
 * Unlike a normal json object, geojson includes all coordinates of a way directly (instead of references)
 * This class is also used by PbfToGml
 * To parse into geojson we first need a json. That is why we are calling PbfToJson first. The response will be used for geojson
 */
public class PbfToGeoJson {
    private JSONObject json; // the json object which came from PbfToJson
    private JSONArray elements; // the elements jsonarray from "json"-attribute
    private JSONObject response; // the geojson object
    private JSONArray features; // features array, similar to elements array, but for response object

    public PbfToGeoJson(Context context, Bundle bundle, InputStream input) {
        PbfToJson featureParser = new PbfToJson(context, bundle, input);
        json = featureParser.getJsonResult();
        response = new JSONObject();
        features = new JSONArray();


        try {
            elements = json.getJSONArray("elements");
            createHeader();

        switch (bundle.getString("type")) {
            case "node":
                insertNodes();
                break;
            case "way":
                insertWays();
                break;
        }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void insertNodes() throws JSONException {
        for (int n = 0; n < elements.length(); n++) {
            JSONObject node = elements.getJSONObject(n);
            JSONObject nodeResult = new JSONObject();
            nodeResult.put("type", "Feature");

            // Property Handling
            JSONObject properties = new JSONObject();
            Iterator<String> iterator = node.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (!key.equals("osm_type")) {
                    // insert keys and values
                    properties.put(key, node.getString(key));
                }
            }

            // Geometry Handling
            JSONObject geometry = new JSONObject();
            geometry.put("type", "Point");
            JSONArray coordinates = new JSONArray();

            coordinates.put(node.getDouble("lat"));
            coordinates.put(node.getDouble("lon"));


            // Put everything
            nodeResult.put("properties", properties);
            geometry.put("coordinates", coordinates);
            nodeResult.put("geometry", geometry);
            features.put(nodeResult);
        }
        response.put("features", features);
    }

    /**
     * creates header of geojson
     */
    private void createHeader() throws JSONException {
        response.put("type", "FeatureCollection");
        response.put("generator", "org.geoit.geocontentprovider");
        response.put("copyright", "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL.");
        response.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    /**
     * if requested type is way, search for nodes in ways
     * @throws JSONException
     */
    private void insertWays() throws JSONException {

        // for each element:
        for (int n = 0; n < elements.length(); n++) {
            JSONObject object = elements.getJSONObject(n);
            // if element is a way
            if (object.getString("osm_type").equals("way")) {

                JSONObject way = new JSONObject();
                way.put("type", "Feature");

                // Property Handling
                JSONObject properties = new JSONObject();
                Iterator<String> iterator = object.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (!key.equals("osm_type") && !key.equals("ref_nodes")) {
                        // insert keys and values
                        properties.put(key, object.getString(key));
                    }
                }


                // Geometry Handling
                JSONObject geometry = new JSONObject();
                geometry.put("type", "LineString");
                JSONArray coordinates = new JSONArray();

                // Get Ids from string (complicated because you cant transport jsonarray via contentvalues)
                String refNodesString = object.getString("ref_nodes");
                Pattern pattern = Pattern.compile("\\[(.*?)\\]", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(refNodesString);
                String[] refNodesStringArray = null;
                JSONArray refNodes = new JSONArray();
                while (matcher.find()) {
                    refNodesStringArray = matcher.group(1).split(",");
                }
                for (String refNode : refNodesStringArray) {
                    refNodes.put(Double.parseDouble(refNode));
                }

                List<double[]> coordinateList = forEachNodeId(refNodes);
                // insert coordinates into geometry tag
                for (double[] coordinate : coordinateList) {
                    JSONArray seperateCoordinates = new JSONArray();
                    seperateCoordinates.put(coordinate[0]);
                    seperateCoordinates.put(coordinate[1]);
                    coordinates.put(seperateCoordinates);
                }

                way.put("properties", properties);
                geometry.put("coordinates", coordinates);
                way.put("geometry", geometry);
                features.put(way);
            }
        }
    response.put("features", features);

    }

    /**
     * for each node id in a way, get lat and lon
     * @param refNodes
     * @return
     * @throws JSONException
     */
    private List<double[]> forEachNodeId(JSONArray refNodes) throws JSONException {
        List<double[]> coordinates = new LinkedList<>();

        // for each node id in way:
        for (int i = 0; i < refNodes.length(); i++) {
            JSONObject node = findNode(refNodes.getLong(i));
            coordinates.add(new double[] {node.getDouble("lat"), node.getDouble("lon")});
        }

        return coordinates;
    }

    private JSONObject findNode(long nodeId) throws JSONException {
        for (int n = 0; n < elements.length(); n++) {
            if (elements.getJSONObject(n).getLong("osm_id") == nodeId) {
                return elements.getJSONObject(n);
            }
        }
        return null;
    }

    public String getResult() {
        return this.response.toString();
    }

    public JSONObject getJsonResult() {
        return this.response;
    }

}
