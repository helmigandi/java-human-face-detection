package com.face_detectin.face_detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {
    private final FaceDetectionService faceDetectionService;
    Logger logger = LoggerFactory.getLogger(AttendanceController.class);

    public AttendanceController(FaceDetectionService faceDetectionService) {
        this.faceDetectionService = faceDetectionService;
    }

    @GetMapping("/clock-in/{imageName}")
    public ResponseEntity<String> clockIn(@PathVariable String imageName) throws IOException {
        boolean isFaceDetected = faceDetectionService.hasHumanFace(imageName);
        if (!isFaceDetected) {
            return ResponseEntity.badRequest()
                    .body("No face detected in the image");
        }

        // Process clock-in logic here
        return ResponseEntity.ok("Clock-in successful");
    }

    @PostMapping(value = "/clock-in")
    public ResponseEntity<String> clockIn(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("username") String username,
            @RequestParam("departmentName") String departmentName,
            @RequestParam("employeeId") String employeeId) {

        try {
            // First check if image has a human face
            boolean hasFace = faceDetectionService.hasHumanFace(photo);

            if (!hasFace) {
                return ResponseEntity.badRequest()
                        .body("No face detected in the image");
            }

            // Process the attendance with user data
            logger.info("Processing clock-in for: {}", username);
            logger.info("Department: {}", departmentName);

            // TODO: Save attendance record to database

            return ResponseEntity.ok("Clock-in successful");
        } catch (Exception e) {
            logger.error("Error processing attendance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing attendance: " + e.getMessage());
        }
    }
}
