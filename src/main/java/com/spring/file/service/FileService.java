package com.spring.file.service;

import com.spring.file.mapper.FileMapper;
import com.spring.file.model.FileDto;
import com.spring.file.model.FileInsertBulkDto;
import com.spring.file.model.FileInsertBulkDto.FileInsertBulkDtoBuilder;
import com.spring.file.model.FileSaveDto;
import com.spring.file.model.FileSaveRequestDto;
import com.spring.file.model.FileSaveResponseDto;
import com.spring.file.model.FileUploadRequestDto;
import com.spring.file.model.FileUploadResponseDto;
import com.spring.file.model.FileUploadedDto;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

  private final FileProperties fileProperties;

  private final FileMapper fileMapper;

  public FileUploadResponseDto upload(FileUploadRequestDto dto) throws IOException {
    String tempPath = getServiceTempPath(dto.getServiceCode());
    List<FileUploadedDto> fileUploadedList = new ArrayList<>();

    for (MultipartFile multipartFile : dto.getFiles()) {
      File file = makeFile(tempPath, multipartFile);
      FileUploadedDto fileUploaded = FileUploadedDto.builder()
          .fileId(file.getName())
          .fileName(FilenameUtils.removeExtension(multipartFile.getOriginalFilename()))
          .fileExtension(FilenameUtils.getExtension(multipartFile.getOriginalFilename()))
          .fileSize(multipartFile.getSize())
          .build();
      fileUploadedList.add(fileUploaded);
    }

    return FileUploadResponseDto.builder()
        .files(fileUploadedList)
        .build();
  }

  public FileSaveResponseDto addBulk(FileSaveRequestDto dto) throws IOException {
    String tempPath = getServiceTempPath(dto.getServiceCode());
    String savePath = getServiceSavePath(dto.getServiceCode());
    FileInsertBulkDtoBuilder fileInsertBulkDtoBuilder = FileInsertBulkDto.builder();

    for (FileSaveDto fileSaveDto : dto.getFiles()) {
      File tempFile = new File(tempPath, fileSaveDto.getFileId());
      File saveFile = new File(savePath, fileSaveDto.getFileId());
      FileUtils.moveFile(tempFile, saveFile);

      fileInsertBulkDtoBuilder.file(FileDto.builder()
          .fileId(fileSaveDto.getFileId())
          .filePath(savePath)
          .fileName(fileSaveDto.getFileName())
          .fileExtension(fileSaveDto.getFileExtension())
          .fileSize(saveFile.length())
          .serviceCode(dto.getServiceCode())
          .tableName(dto.getTableName())
          .distinguishColumnValue(dto.getDistinguishColumnValue())
          .build());
    }

    return FileSaveResponseDto.builder()
        .count(fileMapper.insertBulk(fileInsertBulkDtoBuilder.build()))
        .build();
  }

  private String getServiceTempPath(String serviceCode) {
    final String DELIMITER = "/";
    StringJoiner pathJoiner = new StringJoiner(DELIMITER);
    return pathJoiner
        .add(fileProperties.getTempPath())
        .add(serviceCode)
        .toString()
        .replace(DELIMITER, File.separator);
  }

  private String getServiceSavePath(String serviceCode) {
    final String DELIMITER = "/";
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    StringJoiner pathJoiner = new StringJoiner(DELIMITER);
    return pathJoiner
        .add(fileProperties.getSavePath())
        .add(serviceCode)
        .add(date)
        .toString()
        .replace(DELIMITER, File.separator);
  }

  private File makeFile(String uploadPath, MultipartFile multipartFile) throws IOException {
    String fileName = UUID.randomUUID().toString();
    File file = new File(uploadPath, fileName);
    file.mkdirs();
    multipartFile.transferTo(file);

    return file;
  }

}
