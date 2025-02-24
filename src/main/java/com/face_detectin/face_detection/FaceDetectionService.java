package com.face_detectin.face_detection;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;

@Service
public class FaceDetectionService {
    private final CascadeClassifier cascadeClassifier;
    private final ResourceLoader resourceLoader;

    public FaceDetectionService(ResourceLoader resourceLoader) throws IOException {
        this.resourceLoader = resourceLoader;

        // Load OpenCV native library
        nu.pattern.OpenCV.loadLocally();

        // Initialize classifier
        this.cascadeClassifier = loadClassifier();
    }

    private CascadeClassifier loadClassifier() throws IOException {
        String classifierPath = "haarcascade_frontalface_default.xml";
        Resource classifierResource = resourceLoader.getResource("classpath:" + classifierPath);

        if (!classifierResource.exists()) {
            throw new FileNotFoundException("Classifier file not found in resources: " + classifierPath);
        }

        File classifierFile = File.createTempFile("classifier", ".xml");

        try (InputStream is = classifierResource.getInputStream();
             FileOutputStream fos = new FileOutputStream(classifierFile)) {
            FileCopyUtils.copy(is, fos);
        }

        CascadeClassifier classifier = new CascadeClassifier();
        boolean loaded = classifier.load(classifierFile.getAbsolutePath());

        Files.delete(classifierFile.toPath());

        if (!loaded) {
            throw new IllegalStateException("Failed to load classifier file");
        }

        return classifier;
    }

    public boolean hasHumanFace(String imageName) throws IOException {
        Resource imageResource = resourceLoader.getResource("classpath:images/" + imageName);

        if (!imageResource.exists()) {
            throw new FileNotFoundException("Image not found in resources/images: " + imageName);
        }

        File imageFile = File.createTempFile("image", ".jpg");

        try (InputStream is = imageResource.getInputStream();
             FileOutputStream fos = new FileOutputStream(imageFile)) {
            FileCopyUtils.copy(is, fos);
        }

        Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
        Files.delete(imageFile.toPath());

        if (image.empty()) {
            throw new IllegalArgumentException("Failed to load image: " + imageName);
        }


        // Check image size
//        if (image.width() < 320 || image.height() < 240) {
//            throw new IllegalArgumentException("Image is too small. Minimum size is 320x240 pixels");
//        }
//        if (image.width() > 4096 || image.height() > 4096) {
//            throw new IllegalArgumentException("Image is too large. Maximum size is 4096x4096 pixels");
//        }

        MatOfRect faceDetections = new MatOfRect();

        // Improved detection parameters
        double scaleFactor = 1.1;  // How much the image size is reduced at each image scale
        int minNeighbors = 5;      // Higher number gives less detections but with higher quality
        int flags = 0;             // Parameter indicating the type of Haar feature
        Size minSize = new Size(30, 30);  // Minimum possible face size
        Size maxSize = new Size();        // Maximum possible face size (0,0 means no limit)

        cascadeClassifier.detectMultiScale(
                image,
                faceDetections,
                scaleFactor,
                minNeighbors,
                flags,
                minSize,
                maxSize
        );

        // Check confidence level
        return !faceDetections.empty() && faceDetections.toArray().length > 0;
    }

    public boolean hasHumanFaceBase64(String base64Image) {
        // Remove base64 prefix if exists (e.g., "data:image/jpeg;base64,")
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }

        // Decode base64 to byte array
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // Convert byte array to Mat
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);

        if (image.empty()) {
            throw new IllegalArgumentException("Failed to load image from base64 string");
        }

        MatOfRect faceDetections = new MatOfRect();
        cascadeClassifier.detectMultiScale(image, faceDetections);

        return !faceDetections.empty();
    }
}
