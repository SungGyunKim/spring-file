package com.spring.file.web;

import com.spring.file.model.FileDto;
import com.spring.file.model.FileSaveRequestDto;
import com.spring.file.model.FileSaveResponseDto;
import com.spring.file.model.FileUploadRequestDto;
import com.spring.file.model.FileUploadResponseDto;
import com.spring.file.service.FileService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/files")
public class FileController {

  private final FileService fileService;

  @PostMapping("/upload")
  public ResponseEntity<FileUploadResponseDto> upload(@Valid FileUploadRequestDto dto)
      throws IOException {
    return ResponseEntity.ok(fileService.upload(dto));
  }

  @PostMapping("/save")
  public ResponseEntity<FileSaveResponseDto> save(@Valid @RequestBody FileSaveRequestDto dto)
      throws IOException {
    return ResponseEntity.ok(fileService.save(dto));
  }

  @GetMapping("/{fileId}/download")
  public ResponseEntity<Resource> download(@PathVariable String fileId) throws Exception {
    FileDto fileDto = fileService.findByFileId(fileId);
    String filename = fileDto.getFileNameExtension();
    Path filePath = Path.of(fileDto.getFilePath(), fileDto.getFileId());
    Resource resource = UrlResource.from(filePath.toUri());

    if (!resource.exists()) {
      throw new Exception("파일이 존재하지 않습니다.");
    } else if (!resource.isReadable()) {
      throw new Exception("파일을 읽을 수 없습니다.");
    }

    MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
    String contentDisposition = ContentDisposition.attachment()
        .filename(filename, StandardCharsets.UTF_8)
        .build()
        .toString();

    return ResponseEntity.ok()
        .contentType(contentType)
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
        .body(resource);
  }

  @GetMapping("/{fileId}/inline")
  public ResponseEntity<Resource> inline(@PathVariable String fileId) throws Exception {
    FileDto fileDto = fileService.findByFileId(fileId);
    String filename = fileDto.getFileNameExtension();
    Path filePath = Path.of(fileDto.getFilePath(), fileDto.getFileId());
    Resource resource = UrlResource.from(filePath.toUri());

    if (!resource.exists()) {
      throw new Exception("파일이 존재하지 않습니다.");
    } else if (!resource.isReadable()) {
      throw new Exception("파일을 읽을 수 없습니다.");
    }

    MediaType contentType = MediaTypeFactory.getMediaType(filename)
        .orElseThrow(() -> new Exception("미디어 타입을 확인할 수 없습니다."));
    String contentDisposition = ContentDisposition.inline()
        .filename(filename, StandardCharsets.UTF_8)
        .build()
        .toString();

    return ResponseEntity.ok()
        .contentType(contentType)
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
        .body(resource);
  }

  @GetMapping("/{fileId}/stream")
  public ResponseEntity<ResourceRegion> stream(@PathVariable String fileId,
      @RequestHeader HttpHeaders httpHeaders)
      throws Exception {
    FileDto fileDto = fileService.findByFileId(fileId);
    String filename = fileDto.getFileNameExtension();
    Path filePath = Path.of(fileDto.getFilePath(), fileDto.getFileId());
    Resource resource = UrlResource.from(filePath.toUri());

    long chunkSize = 1024 * 1024;
    HttpRange httpRange = httpHeaders.getRange().stream()
        .findFirst()
        .orElse(HttpRange.createByteRange(0, resource.contentLength() - 1));
    long start = httpRange.getRangeStart(resource.contentLength());
    long end = httpRange.getRangeEnd(resource.contentLength());
    long rangeLength = Long.min(chunkSize, end - start + 1);

    MediaType contentType = MediaTypeFactory.getMediaType(filename)
        .orElse(MediaType.APPLICATION_OCTET_STREAM);
    ResourceRegion resourceRegion = new ResourceRegion(resource, start, rangeLength);

    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
        .contentType(contentType)
        .body(resourceRegion);
  }

}
