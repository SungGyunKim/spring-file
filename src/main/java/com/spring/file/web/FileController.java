package com.spring.file.web;

import com.spring.file.model.FileSaveTempRequestDto;
import com.spring.file.model.FileSaveTempResponseDto;
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

  @PostMapping("/save-temp")
  public ResponseEntity<FileSaveTempResponseDto> saveTemp(FileSaveTempRequestDto dto)
      throws IOException {
    return ResponseEntity.ok(fileService.saveTemp(dto));
  }

}
