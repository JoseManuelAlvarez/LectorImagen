package com.kepollo.mx.lectorimagen;

import android.annotation.SuppressLint;
import android.media.Image;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;

public class YourAnalyzer  implements ImageAnalysis.Analyzer {

    @Override
    public void analyze(ImageProxy imageProxy) {
        @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            }
            // Pass image to an ML Kit Vision API
            // ...
        }
    }
}