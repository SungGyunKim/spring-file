package com.spring.file.service;

import com.spring.file.mapper.FileMapper;
import com.spring.file.model.FileDeleteByFileIdsRequestDto;
import com.spring.file.model.FileDeleteByFileIdsResponseDto;
import com.spring.file.model.FileDeleteByFileIdsResponseDto.FileDeleteByFileIdsResponseDtoBuilder;
import com.spring.file.model.FileDto;
import com.spring.file.model.FileSaveRequestDto;
import com.spring.file.model.FileSaveResponseDto;
import com.spring.file.model.FileUploadRequestDto;
import com.spring.file.model.FileUploadResponseDto;
import com.spring.file.model.FileUploadedDto;
import com.spring.file.properties.FileProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

  private final FileProperties fileProperties;

  private final StandardPBEByteEncryptor fileEncryptor;

  private final FileMapper fileMapper;

  public FileUploadResponseDto upload(FileUploadRequestDto dto) throws Exception {
    String tempPath = getServiceTempPath(dto.getServiceCode());
    List<FileUploadedDto> fileUploadedList = new ArrayList<>();

    for (MultipartFile multipartFile : dto.getFiles()) {
      String fileId = makeFile(tempPath, multipartFile);
      FileUploadedDto fileUploaded = FileUploadedDto.builder()
          .fileId(fileId)
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

  @Transactional
  public FileSaveResponseDto save(FileSaveRequestDto dto) throws IOException {
    String tempPath = getServiceTempPath(dto.getServiceCode());
    String savePath = getServiceSavePath(dto.getServiceCode());

    List<FileDto> savedFileList = fileMapper.findByServiceCodeAndTableNameAndDistinguishColumnValue(
        FileDto.builder().
            serviceCode(dto.getServiceCode())
            .tableName(dto.getTableName())
            .distinguishColumnValue(dto.getDistinguishColumnValue())
            .build());

    // target
    List<FileDto> insertFileList = dto.getFiles().stream()
        .filter(file -> savedFileList.stream()
            .noneMatch(savedFile -> savedFile.getFileId().equals(file.getFileId()))
        )
        .map(file -> FileDto.builder()
            .fileId(file.getFileId())
            .filePath(savePath)
            .fileName(file.getFileName())
            .fileExtension(file.getFileExtension())
            .fileSize(file.getFileSize())
            .serviceCode(dto.getServiceCode())
            .tableName(dto.getTableName())
            .distinguishColumnValue(dto.getDistinguishColumnValue())
            .build()
        )
        .toList();
    List<FileDto> deleteFileList = savedFileList.stream()
        .filter(savedFile -> dto.getFiles().stream()
            .noneMatch(file -> savedFile.getFileId().equals(file.getFileId()))
        ).toList();
    List<FileDto> maintainedList = savedFileList.stream()
        .filter(savedFile -> dto.getFiles().stream()
            .anyMatch(file -> savedFile.getFileId().equals(file.getFileId()))
        ).toList();

    // database
    if (!ObjectUtils.isEmpty(insertFileList)) {
      fileMapper.insertBulk(insertFileList);
    }
    if (!ObjectUtils.isEmpty(deleteFileList)) {
      fileMapper.deleteBulk(deleteFileList.stream()
          .map(FileDto::getFileId)
          .toList()
      );
    }

    // file
    for (FileDto fileDto : insertFileList) {
      File tempFile = new File(tempPath, fileDto.getFileId());
      File saveFile = new File(savePath, fileDto.getFileId());
      FileUtils.moveFile(tempFile, saveFile);
    }
    for (FileDto fileDto : deleteFileList) {
      File directory = new File(fileDto.getFilePath());
      File file = FileUtils.getFile(directory, fileDto.getFileId());

      FileUtils.delete(file);
      if (ObjectUtils.isEmpty(directory.list())) {
        directory.delete();
      }
    }

    return FileSaveResponseDto.builder()
        .insertedList(insertFileList)
        .deletedList(deleteFileList)
        .maintainedList(maintainedList)
        .build();
  }

  @Transactional
  public FileDeleteByFileIdsResponseDto deleteByFileIds(FileDeleteByFileIdsRequestDto dto)
      throws IOException {
    FileDeleteByFileIdsResponseDtoBuilder responseBuilder = FileDeleteByFileIdsResponseDto.builder();
    List<FileDto> fileDtoList = fileMapper.findByFileIds(dto.getFileIds());
    int deletedCount = fileMapper.deleteBulk(dto.getFileIds());
    deleteFile(fileDtoList);

    return responseBuilder
        .count(deletedCount)
        .build();
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public FileDto findByFileId(String fileId) {
    return fileMapper.findByFileId(fileId);
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

  private String makeFile(String uploadPath, MultipartFile multipartFile) throws IOException {
    String fileName = UUID.randomUUID().toString();
    byte[] encrypted = fileEncryptor.encrypt(multipartFile.getBytes());
    Path path = Paths.get(uploadPath, fileName);
    Files.write(path, encrypted);

    return fileName;
  }

  public Resource getResource(FileDto fileDto) throws Exception {
    Path path = Paths.get(fileDto.getFilePath(), fileDto.getFileId());
    byte[] decrypted = fileEncryptor.decrypt(Files.readAllBytes(path));
    Resource resource = new ByteArrayResource(decrypted);

    if (!resource.exists()) {
      throw new Exception("파일이 존재하지 않습니다.");
    } else if (!resource.isReadable()) {
      throw new Exception("파일을 읽을 수 없습니다.");
    }

    return resource;
  }

  private void deleteFile(List<FileDto> fileDtoList) throws IOException {
    for (FileDto fileDto : fileDtoList) {
      File directory = new File(fileDto.getFilePath());
      File file = FileUtils.getFile(directory, fileDto.getFileId());

      FileUtils.delete(file);
      if (ObjectUtils.isEmpty(directory.list())) {
        directory.delete();
      }
    }
  }

}
