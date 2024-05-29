package com.spring.file.service;

import com.spring.file.model.FileDto;
import com.spring.file.model.FileSaveTempRequestDto;
import com.spring.file.model.FileSaveTempResponseDto;
import com.spring.file.properties.FileProperties;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

  private final FileProperties fileProperties;

  public FileSaveTempResponseDto saveTemp(FileSaveTempRequestDto dto) throws IOException {
    List<FileDto> fileDtoList = new ArrayList<>();
    String uploadPath = makeFolder(fileProperties.getSaveTempPath(), dto.getServiceName());

    for (MultipartFile multipartFile : dto.getFiles()) {
      File file = makeFile(uploadPath, multipartFile);
      FileDto fileDto = FileDto.builder()
          .id(file.getName())
          .name(FilenameUtils.removeExtension(multipartFile.getOriginalFilename()))
          .extension(FilenameUtils.getExtension(multipartFile.getOriginalFilename()))
          .size(multipartFile.getSize())
          .build();
      fileDtoList.add(fileDto);
    }

    return FileSaveTempResponseDto.builder().files(fileDtoList).build();
  }

  private String makeFolder(String basePath, String serviceName) {
    final String DELIMITER = "/";
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    StringJoiner pathJoiner = new StringJoiner(DELIMITER);
    String path = pathJoiner
        .add(basePath)
        .add(serviceName)
        .add(date)
        .toString()
        .replace(DELIMITER, File.separator);
    File folder = new File(path);
    folder.mkdirs();

    return path;
  }

  private File makeFile(String uploadPath, MultipartFile multipartFile) throws IOException {
    String fileName = UUID.randomUUID().toString();
    File file = new File(uploadPath, fileName);
    multipartFile.transferTo(file);

    return file;
  }

}
