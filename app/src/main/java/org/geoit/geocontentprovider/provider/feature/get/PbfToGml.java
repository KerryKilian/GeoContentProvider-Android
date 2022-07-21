package org.geoit.geocontentprovider.provider.feature.get;

import android.content.Context;
import android.os.Bundle;

import org.json.JSONObject;

import java.io.InputStream;

/**
 * not finished yet
 */
public class PbfToGml {
    JSONObject json = new JSONObject();

    public PbfToGml(Context context, Bundle bundle, InputStream input) {
        PbfToGeoJson pbfToGeoJson = new PbfToGeoJson(context, bundle, input);
        this.json = pbfToGeoJson.getJsonResult();
    }


}
