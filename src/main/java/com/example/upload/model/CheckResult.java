package com.example.upload.model;

import lombok.Data;

@Data
public class CheckResult {
    private boolean exists;
    private int uploadedChunks;
    private String message;
}