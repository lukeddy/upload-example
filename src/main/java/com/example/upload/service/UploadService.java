package com.example.upload.service;

import com.example.upload.model.CheckResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
public class UploadService {

    @Value("${storage.path:./uploads}")
    private String storagePath;

    @Value("${storage.chunk-size:5242880}")
    private int chunkSize;

    private final Map<String, List<Integer>> uploadProgress = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            File dir = new File(storagePath).getCanonicalFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            storagePath = dir.getAbsolutePath();
            log.info("上传目录初始化: {}", storagePath);
        } catch (IOException e) {
            log.error("上传目录初始化失败: {}", e.getMessage());
        }
    }

    public String calculateMd5(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            return bytesToHex(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("计算MD5失败: {}", e.getMessage());
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public CheckResult checkFileExists(String fileMd5, String fileName, long fileSize) {
        CheckResult result = new CheckResult();
        File uploadDir = new File(storagePath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File mergedFile = new File(uploadDir, fileMd5 + "_" + fileName);
        if (mergedFile.exists()) {
            log.info("秒传成功: 文件{}已存在, path={}", fileName, mergedFile.getPath());
            result.setExists(true);
            result.setMessage("文件已上传完成");
            return result;
        }

        File chunksDir = new File(uploadDir, fileMd5);
        if (chunksDir.exists() && chunksDir.isDirectory()) {
            File[] chunks = chunksDir.listFiles((dir, name) -> name.startsWith("chunk_"));
            Set<Integer> uploaded = new HashSet<>();
            if (chunks != null) {
                for (File chunk : chunks) {
                    String name = chunk.getName();
                    int num = Integer.parseInt(name.substring(6));
                    uploaded.add(num);
                }
            }
            log.info("断点续传: 已上传分片数={}, 总分片数={}", uploaded.size(), (fileSize + chunkSize - 1) / chunkSize);
            result.setExists(false);
            result.setUploadedChunks(uploaded.size());
            result.setMessage("需要继续上传");
            uploadProgress.put(fileMd5, new ArrayList<>(uploaded));
        } else {
            log.info("新文件上传: {}", fileName);
            result.setExists(false);
            result.setUploadedChunks(0);
            result.setMessage("新���件");
        }
        return result;
    }

    public String uploadChunk(MultipartFile chunk, String fileMd5, String fileName, int chunkNumber) {
        try {
            File uploadDir = new File(storagePath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
                log.info("创建上传目录: {}", uploadDir.getPath());
            }

            File chunksDir = new File(uploadDir, fileMd5);
            if (!chunksDir.exists()) {
                chunksDir.mkdirs();
                log.info("创建分片目录: {}", chunksDir.getPath());
            }

            File chunkFile = new File(chunksDir, "chunk_" + chunkNumber);
            chunk.transferTo(chunkFile);
            log.info("接收分片: chunkNumber={}, size={}, path={}", chunkNumber, chunk.getSize(), chunkFile.getPath());

            synchronized (uploadProgress) {
                uploadProgress.computeIfAbsent(fileMd5, k -> new ArrayList<>()).add(chunkNumber);
            }

            return "分片上传成功";
        } catch (IOException e) {
            log.error("分片上传失败: {}", e.getMessage());
            return "分片上传失败: " + e.getMessage();
        }
    }

    public String mergeChunks(String fileMd5, String fileName, long fileSize) {
        File uploadDir = new File(storagePath);
        File chunksDir = new File(uploadDir, fileMd5);
        File mergedFile = new File(uploadDir, fileMd5 + "_" + fileName);

        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);
        log.info("开始合并分片: totalChunks={}", totalChunks);

        try (FileOutputStream fos = new FileOutputStream(mergedFile);
             FileChannel channel = fos.getChannel()) {

            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(chunksDir, "chunk_" + i);
                if (!chunkFile.exists()) {
                    log.error("分片不存在: chunk_{}", i);
                    return "分片不存在: chunk_" + i;
                }

                try (FileInputStream fis = new FileInputStream(chunkFile);
                     FileChannel inChannel = fis.getChannel()) {
                    inChannel.transferTo(0, inChannel.size(), channel);
                }
                chunkFile.delete();
            }

            chunksDir.delete();
            log.info("文件合并完成: {}", mergedFile.getPath());

            synchronized (uploadProgress) {
                uploadProgress.remove(fileMd5);
            }

            return "文件合并完成";
        } catch (IOException e) {
            log.error("合并失败: {}", e.getMessage());
            return "合并失败: " + e.getMessage();
        }
    }

    public int getChunkSize() {
        return chunkSize;
    }
}