package com.spring.file.web;

import com.spring.file.model.FileUploadRequestDto;
import com.spring.file.model.FileUploadResponseDto;
import com.spring.file.service.FileService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/files")
public class FileController {

  private final FileService fileService;

  @PostMapping("/upload")
  public ResponseEntity<FileUploadResponseDto> upload(FileUploadRequestDto dto)
      throws IOException {
    return ResponseEntity.ok(fileService.upload(dto));
  }

}
