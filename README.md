# 大文件分片上传示例

演示项目，实现大文件的分片上传、断点续传、秒传功能。

## 技术栈

- JDK 21
- Spring Boot 3.2
- 前端: HTML + TailwindCSS CDN + SparkMD5

## 功能特性

1. **分片上传** - 大文件分成5MB小分片依次上传
2. **断点续传** - 上传中断后重新选择文件自动续传
3. **秒传** - 相同MD5文件直接完成，无需重复上传
4. **暂停/继续** - 支持暂停和继续上传

## 启动方式

```bash
mvn spring-boot:run
```

访问地址: http://localhost:8080

## 文件存储

上传文件保存在 `./uploads` 目录

## 接口说明

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/upload/chunkSize` | GET | 获取分片大小 |
| `/api/upload/check` | POST | 检查文件(MD5、秒传、已上传分片) |
| `/api/upload/chunk` | POST | 上传单个分片 |
| `/api/upload/merge` | POST | 合并所有分片 |