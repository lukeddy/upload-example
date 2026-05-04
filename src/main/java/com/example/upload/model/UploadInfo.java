package com.example.upload.model;

import lombok.Data;

@Data
public class UploadInfo {
    private String fileName;
    private String fileMd5;
    private long fileSize;
    private int chunkSize;
    private int totalChunks;
    private int chunkNumber;
}