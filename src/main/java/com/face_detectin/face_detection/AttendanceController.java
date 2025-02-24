package com.face_detectin.face_detection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {
    private final FaceDetectionService faceDetectionService;

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
}
