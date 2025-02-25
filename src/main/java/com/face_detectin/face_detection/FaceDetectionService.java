package com.face_detectin.face_detection;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;

@Service
public class FaceDetectionService {
    private final CascadeClassifier cascadeClassifier;
    private final ResourceLoader resourceLoader;

    Logger logger = LoggerFactory.getLogger(FaceDetectionService.class);

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

//    public boolean hasHumanFace(String imageName) throws IOException {
//        Resource imageResource = resourceLoader.getResource("classpath:images/" + imageName);
//
//        if (!imageResource.exists()) {
//            throw new FileNotFoundException("Image not found in resources/images: " + imageName);
//        }
//
//        File imageFile = File.createTempFile("image", ".jpg");
//
//        try (InputStream is = imageResource.getInputStream();
//             FileOutputStream fos = new FileOutputStream(imageFile)) {
//            FileCopyUtils.copy(is, fos);
//        }
//
//        Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
//        Files.delete(imageFile.toPath());
//
//        if (image.empty()) {
//            throw new IllegalArgumentException("Failed to load image: " + imageName);
//        }
//
//
//        // Check image size
////        if (image.width() < 320 || image.height() < 240) {
////            throw new IllegalArgumentException("Image is too small. Minimum size is 320x240 pixels");
////        }
////        if (image.width() > 4096 || image.height() > 4096) {
////            throw new IllegalArgumentException("Image is too large. Maximum size is 4096x4096 pixels");
////        }
//
//        MatOfRect faceDetections = new MatOfRect();
//
//        // Improved detection parameters
//        double scaleFactor = 1.1;  // How much the image size is reduced at each image scale
//        int minNeighbors = 5;      // Higher number gives less detections but with higher quality
//        int flags = 0;             // Parameter indicating the type of Haar feature
//        Size minSize = new Size(30, 30);  // Minimum possible face size
//        Size maxSize = new Size();        // Maximum possible face size (0,0 means no limit)
//
//        cascadeClassifier.detectMultiScale(
//                image,
//                faceDetections,
//                scaleFactor,
//                minNeighbors,
//                flags,
//                minSize,
//                maxSize
//        );
//
//        // Check confidence level
//        return !faceDetections.empty() && faceDetections.toArray().length > 0;
//    }

    public boolean hasHumanFace(MultipartFile photo) throws IOException {
        // Validate file type
        String contentType = photo.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Only JPEG and PNG images are supported");
        }

        // Validate file size (max 5MB)
        if (photo.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 5MB");
        }

        // Convert MultipartFile to byte array
        byte[] imageBytes = photo.getBytes();

        // Convert byte array to Mat
        Mat originalImage = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);

        // Validate image
        if (originalImage.empty()) {
            throw new IllegalArgumentException("Failed to load image from uploaded file");
        }

        // Check minimum image size
        if (originalImage.width() < 320 || originalImage.height() < 240) {
            throw new IllegalArgumentException("Image is too small. Minimum size is 320x240 pixels");
        }

        // Resize image for optimal face detection
        Mat processImage = resizeImageForFaceDetection(originalImage);

        // Perform face detection
        MatOfRect faceDetections = new MatOfRect();

        // Improved detection parameters
        double scaleFactor = 1.1;
        int minNeighbors = 5;
        int flags = 0;
        Size minSize = new Size(30, 30);
        Size maxSize = new Size();

        cascadeClassifier.detectMultiScale(
                processImage,
                faceDetections,
                scaleFactor,
                minNeighbors,
                flags,
                minSize,
                maxSize
        );

        // If we're working with a resized image, free it to conserve memory
        if (processImage != originalImage) {
            processImage.release();
        }

        // Free the original image
        originalImage.release();

        return !faceDetections.empty() && faceDetections.toArray().length > 0;
    }

    // Similar implementation for the base64 version
    public boolean hasHumanFace(String base64Image) {
        // Remove base64 prefix if exists
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }

        // Decode base64 to byte array
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // Convert byte array to Mat
        Mat originalImage = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);

        // Validate image
        if (originalImage.empty()) {
            throw new IllegalArgumentException("Failed to load image from base64 string");
        }

        // Check minimum image size
        if (originalImage.width() < 320 || originalImage.height() < 240) {
            throw new IllegalArgumentException("Image is too small. Minimum size is 320x240 pixels");
        }

        // Resize image for optimal face detection
        Mat processImage = resizeImageForFaceDetection(originalImage);

        // Perform face detection
        MatOfRect faceDetections = new MatOfRect();

        double scaleFactor = 1.1;
        int minNeighbors = 5;
        int flags = 0;
        Size minSize = new Size(30, 30);
        Size maxSize = new Size();

        cascadeClassifier.detectMultiScale(
                processImage,
                faceDetections,
                scaleFactor,
                minNeighbors,
                flags,
                minSize,
                maxSize
        );

        // Free memory
        if (processImage != originalImage) {
            processImage.release();
        }
        originalImage.release();

        return !faceDetections.empty() && faceDetections.toArray().length > 0;
    }

    /**
     * Resizes an image for optimal face detection performance.
     * OpenCV face detection works most efficiently on images around 480-720px on the longest side.
     */
    private Mat resizeImageForFaceDetection(Mat originalImage) {
        // Skip resizing if image is already small enough
        if (originalImage.width() <= 720 && originalImage.height() <= 720) {
            return originalImage;
        }

        // Calculate new dimensions while maintaining aspect ratio
        double aspectRatio = (double) originalImage.width() / originalImage.height();
        int newWidth, newHeight;

        if (aspectRatio >= 1.0) {
            // Landscape or square image
            newWidth = 720;
            newHeight = (int) (newWidth / aspectRatio);
        } else {
            // Portrait image
            newHeight = 720;
            newWidth = (int) (newHeight * aspectRatio);
        }

        // Create a new Mat for the resized image
        Mat resizedImage = new Mat();

        // Resize the image
        Imgproc.resize(originalImage, resizedImage, new Size(newWidth, newHeight), 0, 0, Imgproc.INTER_AREA);

        logger.debug("Image resized from {}x{} to {}x{}",
                originalImage.width(), originalImage.height(), resizedImage.width(), resizedImage.height());

        return resizedImage;
    }
}
