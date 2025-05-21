package com.kit.photocapture.model.loader;

import android.content.Context;

import com.kit.photocapture.model.type.ModelType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RawModelLoader extends ModelLoader {

    public RawModelLoader(Context context, ModelType modelType) {
        super(context, modelType);
    }

    @Override
    protected byte[] loadBytes() throws IOException {
        try (InputStream is = context.getResources().openRawResource(modelType.getResId());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            return baos.toByteArray();
        }
    }
}

