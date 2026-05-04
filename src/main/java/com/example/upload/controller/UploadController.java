package com.example.upload.controller;

import com.example.upload.model.CheckResult;
import com.example.upload.model.UploadInfo;
import com.example.upload.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @GetMapping("/chunkSize")
    public ResponseEntity<Integer> getChunkSize() {
        log.info("获取分片大小");
        return ResponseEntity.ok(uploadService.getChunkSize());
    }

    @PostMapping("/check")
    public ResponseEntity<CheckResult> checkFile(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") long fileSize) {
        log.info("检查文件: fileName={}, fileSize={}, fileMd5={}", fileName, fileSize, fileMd5);
        CheckResult result = uploadService.checkFileExists(fileMd5, fileName, fileSize);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/chunk")
    public ResponseEntity<Map<String, String>> uploadChunk(
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("fileName") String fileName,
            @RequestParam("chunkNumber") int chunkNumber) {
        log.info("上传分片: chunkNumber={}, fileName={}", chunkNumber, fileName);
        String result = uploadService.uploadChunk(chunk, fileMd5, fileName, chunkNumber);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/merge")
    public ResponseEntity<Map<String, String>> mergeChunks(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") long fileSize) {
        log.info("合并分片: fileName={}, fileSize={}", fileName, fileSize);
        String result = uploadService.mergeChunks(fileMd5, fileName, fileSize);
        return ResponseEntity.ok(Map.of("message", result));
    }
}