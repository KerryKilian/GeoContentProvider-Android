package org.geoit.geocontentprovider.provider.feature.get;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.wolt.osm.parallelpbf.entity.Info;
import com.wolt.osm.parallelpbf.entity.Node;
import com.wolt.osm.parallelpbf.entity.Way;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * not finished yet
 */
public class GmlCreator {
    private StringBuilder result;
    private static final String TAG = "GeoProvider";


    public GmlCreator(double minLat, double minLon, double maxLat, double maxLon) {
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

    public void insertNode(JSONObject node) throws JSONException {
        Iterator<String> iterator = node.keys();

        append("\n<gml:Point");
        appendKey("id", String.valueOf(node.getLong("id")));
        append(">");

        append("<gml:pos srsDimension=\"2\">" +
                node.getDouble("lat") + " " +
                node.getDouble("lon") + "</gml:pos>");




/*
        while (iterator.hasNext()) {
            append("\n<tag")
        }
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
*/

    }

    public void insertWay(Way way) {
        Info info = way.getInfo();
        Map<String, String> tags = way.getTags();
        append("<way");
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
