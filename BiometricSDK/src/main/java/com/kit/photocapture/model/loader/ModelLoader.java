package com.kit.photocapture.model.loader;

import android.content.Context;
import android.util.Log;

import com.kit.photocapture.model.type.ModelType;

import java.io.IOException;

public abstract class ModelLoader {

    protected final Context context;
    protected final ModelType modelType;

    public ModelLoader(Context context, ModelType modelType) {
        this.context = context;
        this.modelType = modelType;
    }

    public byte[] loadAsByteArray() {
        try {
            return loadBytes();
        } catch (IOException e) {
            Log.e("ModelLoader", "Failed to load: " + modelType.getFileName(), e);
            return null;
        }
    }

    protected abstract byte[] loadBytes() throws IOException;
}

