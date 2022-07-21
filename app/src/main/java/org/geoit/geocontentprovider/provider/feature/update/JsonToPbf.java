package org.geoit.geocontentprovider.provider.feature.update;

import static com.wolt.osm.parallelpbf.entity.RelationMember.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wolt.osm.parallelpbf.ParallelBinaryWriter;
import com.wolt.osm.parallelpbf.entity.Node;
import com.wolt.osm.parallelpbf.entity.OsmEntity;
import com.wolt.osm.parallelpbf.entity.Relation;
import com.wolt.osm.parallelpbf.entity.RelationMember;
import com.wolt.osm.parallelpbf.entity.Way;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * This class is used for inserting values - that were send from client - into a pbf file.
 * I am using the Parallel OSM PBF Parser from Denis Chaplygin and Scott Crosby
 * @author Kilian Aaron Brinkner
 */
public class JsonToPbf {
    JsonObject request;
    JsonArray elements;
    OutputStream output;
    private int idAutomatic = -1;

    /**
     * Constructor, called from api
     * @param bundle
     * @param context
     */
    public JsonToPbf(Bundle bundle, Context context) {
        // Another json library because gson can convert from string to json
        request = new Gson().fromJson(bundle.getString("content"), JsonObject.class);
        elements = request.getAsJsonArray("elements");
        log(elements.toString());

        output = createPbf(context, bundle.getString("id"));
        parseJson();
    }

    /**
     * creates .pbf file under  data/user/0/org.geoit.geocontentprovider/app_XXX.pbf
     * @param context
     * @param name
     * @return
     */
    private OutputStream createPbf(Context context, String name) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir(name, Context.MODE_PRIVATE);
        if (directory.exists()) {
            directory.delete();
        }
        OutputStream output = null;
        try {
            directory.createNewFile();
            output = new FileOutputStream(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    private void parseJson() {
        ParallelBinaryWriter writer = new ParallelBinaryWriter(output, 1, null);
        writer.start();
        log("Now inserting features from client");

        // for each node element in json
        for (int n = 0; n < elements.size(); n++) {
            JsonObject jsonObject = elements.get(n).getAsJsonObject();
            String type = jsonObject.get("type").getAsString();
            switch (type) {
                case "node":
                    Node node = insertInfoNode(jsonObject);
                    node = (Node) insertTags(jsonObject, node);
                    writer.write(node);
                    break;
                case "way":
                    Way way = insertInfoWay(jsonObject);
                    way = (Way) insertTags(jsonObject, way);
                    writer.write(way);
                    break;
                case "relation":
                    Relation relation = insertInfoRelation(jsonObject);
                    relation = (Relation) insertTags(jsonObject, relation);
                    writer.write(relation);
                    break;
            }
        }

        writer.close();
    }

    /**
     * inserts info for node (id, lat, lon)
     * @param json
     * @return
     */
    private Node insertInfoNode(JsonObject json) {
        double lat = json.get("lat").getAsDouble();
        double lon = json.get("lon").getAsDouble();
        Long id = json.get("id").getAsLong();
        Node node;
        if (id != null) {
            node = new Node(id, lat, lon);
        } else {
            node = new Node(idAutomatic, lat, lon);
        }
        idAutomatic--;
        return node;
    }

    /**
     * inserts info for way (id)
     * @param json
     * @return
     */
    private Way insertInfoWay(JsonObject json) {
        Long id = json.get("id").getAsLong();
        JsonArray nodes = json.get("nodes").getAsJsonArray();
        Way way;
        if (id != null) {
            way = new Way(id);
        } else {
            way = new Way(idAutomatic);
        }

        // Get Node ids references
        for (int n = 0; n < nodes.size(); n++) {
            way.getNodes().add(nodes.get(n).getAsLong());
        }

        idAutomatic--;
        return way;
    }

    /**
     * inserts info for relation (id)
     * @param json
     * @return
     */
    private Relation insertInfoRelation(JsonObject json) {
        Long id = json.get("id").getAsLong();
        JsonArray members = json.get("members").getAsJsonArray();
        Relation relation;
        // add ID
        if (id != null) {
            relation = new Relation(id);
        } else {
            relation = new Relation(idAutomatic);
        }

        // Get Member references
        for (int n = 0; n < members.size(); n++) {
            JsonObject element = members.get(n).getAsJsonObject();
            Type type = Type.valueOf(element.get("type").getAsString());
            Long ref = element.get("ref").getAsLong();
            String role = element.get("role").getAsString();
            relation.getMembers().add(new RelationMember(ref, role, type));
        }
        return relation;
    }

    /**
     * inserts tags into Entity-Object
     * @param json
     * @param entity
     * @return OsmEntity
     */
    private OsmEntity insertTags(JsonObject json, OsmEntity entity) {
        JsonObject tags = json.getAsJsonObject("tags");
        Iterator<String> iterator = tags.keySet().iterator();
        log(tags.toString());
        while (iterator.hasNext()) {
            String element = iterator.next();
            entity.getTags().put(element, tags.get(element).getAsString());
        }
        return entity;
    }

    private void log(String string) {
        if (string != null) {
            Log.d("GeoProvider", string);
        }
    }
}
