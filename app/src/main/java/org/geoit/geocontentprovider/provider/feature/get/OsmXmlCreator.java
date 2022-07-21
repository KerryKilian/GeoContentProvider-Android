package org.geoit.geocontentprovider.provider.feature.get;

import android.os.Build;
import android.util.Log;

import com.wolt.osm.parallelpbf.entity.Info;
import com.wolt.osm.parallelpbf.entity.Node;
import com.wolt.osm.parallelpbf.entity.OsmEntity;
import com.wolt.osm.parallelpbf.entity.Way;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Kilian Aaron Brinkner
 * This class creates an osm-xml object. Parseing from pbf to xml file is in PbfToOsm
 */
public class OsmXmlCreator {
    private StringBuilder result;
    private static final String TAG = "GeoProvider";


    public OsmXmlCreator(double minLat, double minLon, double maxLat, double maxLon) {
        result = new StringBuilder();
        result.append("<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<osm version=\"0.6\" generator=\"org.geoit.geocontentprovider\" timestamp=\"" +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\">");
        insertBounds(minLat, minLon, maxLat, maxLon);
    }

    public void insertBounds(double minLat, double minLon, double maxLat, double maxLon) {
        result.append("\n<bounds " +
                "minlat='" + minLat + "' " +
                "minlon='" + minLon + "' " +
                "maxlat='" + maxLat + "' " +
                "maxlon='" + maxLon + "' />");
    }

    public void insertNode(Node node) {
        Info info = node.getInfo();
        Map<String, String> tags = node.getTags();
        append("\n<node");
        appendKey("id", String.valueOf(node.getId()));
        appendKey("timestamp", String.valueOf(info.getTimestamp()));
        appendKey("uid", String.valueOf(info.getUid()));
        appendKey("user", String.valueOf(info.getUsername()));
        appendKey("version", String.valueOf(info.getVersion()));
        appendKey("changeset", String.valueOf(info.getChangeset()));

        appendKey("lat", String.valueOf(node.getLat()));
        appendKey("lon", String.valueOf(node.getLon()));

        if (tags.isEmpty()) {
            append(" />");
        } else {
            append(" >");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tags.forEach((key, value) -> {
                    append("\n<tag " + key + "='" + value + "' />");
                });
            }
        }
        append("\n</node>");


    }

    public void insertWay(Way way) {
        Info info = way.getInfo();
        Map<String, String> tags = way.getTags();
        append("\n<way");
        appendKey("id", String.valueOf(way.getId()));
        appendKey("timestamp", String.valueOf(info.getTimestamp()));
        appendKey("uid", String.valueOf(info.getUid()));
        appendKey("user", String.valueOf(info.getUsername()));
        appendKey("version", String.valueOf(info.getVersion()));
        appendKey("changeset", String.valueOf(info.getChangeset()));

        List<Long> nodes = way.getNodes();
        if (!nodes.isEmpty()) {
            for (int n = 0; n < nodes.size(); n++) {
                append("<nd ref='" + nodes.get(n) + "' />");
            }
        }


        if (tags.isEmpty()) {
            append(" />");
        } else {
            append(" >");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tags.forEach((key, value) -> {
                    append("<tag " + key + "='" + value + "' />\n");
                });
            }
        }
        append("</way>\n");
    }

    public void insertJsonNode(JSONObject node) throws JSONException {
        append("\n<node");
        appendKey("id", node.getString("id"));
        appendKey("lat", node.getString("lat"));
        appendKey("lon", node.getString("lon"));

        JSONObject tags = null;
        try {
            tags = node.getJSONObject("tags");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (tags == null || tags.length() == 0) {
            append(" />");
        } else {
            append(" >");

            Iterator<String> keys = tags.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                append("\n<tag " + key + "='" + tags.getString(key) + "' />");
            }

            append("\n</node>");
        }
    }

    public void insertJsonWay(JSONObject way) throws JSONException {
        append("\n<way");
        appendKey("id", way.getString("id"));


        JSONObject tags = null;

        // tags
        try {
            tags = way.getJSONObject("tags");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (tags == null || tags.length() == 0) {
            append(" />");
        } else {
            append(" >");

            Iterator<String> keys = tags.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                append("\n<tag " + key + "='" + tags.getString(key) + "' />");
            }

            append("\n</way>");
        }


        // node ids
        JSONArray nodes = new JSONArray();
        try {
            nodes = way.getJSONArray("nodes");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (nodes == null || nodes.length() == 0) {
            append(" />");
        } else {
            append(" >");

            for (int n = 0; n < nodes.length(); n++) {
                append("<nd ref='" + nodes.get(n) + "' />");
            }

            append("\n</way>");
        }


    }

    /**
     * @return String in osm-xml format
     */
    public String getOsmXmlResult() {
        result.append("</osm>");
        return result.toString();
    }

    private void appendKey(String key, String value) {
        result.append(" " + key + "='" + value + "'");
    }

    private void append(String string) {
        result.append(string);
    }

    public String toString() {
        return result.toString();
    }

}
