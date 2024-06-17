package com.spring.file.service;

import com.spring.file.mapper.FileMapper;
import com.spring.file.model.FileDeleteByFileIdsRequestDto;
import com.spring.file.model.FileDeleteByFileIdsResponseDto;
import com.spring.file.model.FileDeleteByServiceRequestDto;
import com.spring.file.model.FileDeleteByServiceResponseDto;
import com.spring.file.model.FileDto;
import com.spring.file.model.FileFindByServiceRequestDto;
import com.spring.file.model.FileFindByServiceResponseDto;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
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

    List<FileDto> savedFileList = fileMapper.findByService(
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
            .build())
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
      fileMapper.deleteByFileIds(deleteFileList.stream()
          .map(FileDto::getFileId)
          .toList());
    }

    // file
    for (FileDto fileDto : insertFileList) {
      File tempFile = new File(tempPath, fileDto.getFileId());
      File saveFile = new File(savePath, fileDto.getFileId());

      FileUtils.moveFile(tempFile, saveFile);
    }
    deleteFile(deleteFileList);

    return FileSaveResponseDto.builder()
        .insertedList(insertFileList)
        .deletedList(deleteFileList)
        .maintainedList(maintainedList)
        .build();
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public FileDto findByFileId(String fileId) {
    return fileMapper.findByFileId(fileId);
  }

  @Transactional
  public FileDeleteByFileIdsResponseDto deleteByFileIds(FileDeleteByFileIdsRequestDto dto)
      throws IOException {
    int deletedCount = 0;
    List<FileDto> fileDtoList = fileMapper.findByFileIds(dto.getFileIds());

    if (!ObjectUtils.isEmpty(fileDtoList)) {
      deletedCount = fileMapper.deleteByFileIds(dto.getFileIds());
      deleteFile(fileDtoList);
    }

    return FileDeleteByFileIdsResponseDto.builder()
        .count(deletedCount)
        .build();
  }


  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public FileFindByServiceResponseDto findByService(FileFindByServiceRequestDto dto) {
    FileDto params = FileDto.builder()
        .serviceCode(dto.getServiceCode())
        .tableName(dto.getTableName())
        .distinguishColumnValue(dto.getDistinguishColumnValue())
        .build();
    List<FileDto> result = fileMapper.findByService(params);

    return FileFindByServiceResponseDto.builder()
        .files(result)
        .build();
  }

  @Transactional
  public FileDeleteByServiceResponseDto deleteByService(FileDeleteByServiceRequestDto dto)
      throws IOException {
    int deletedCount = 0;

    FileDto params = FileDto.builder()
        .serviceCode(dto.getServiceCode())
        .tableName(dto.getTableName())
        .distinguishColumnValue(dto.getDistinguishColumnValue())
        .build();
    List<FileDto> fileDtoList = fileMapper.findByService(params);

    if (ObjectUtils.isEmpty(fileDtoList)) {
      deletedCount = fileMapper.deleteByService(params);
      deleteFile(fileDtoList);
    }

    return FileDeleteByServiceResponseDto.builder()
        .count(deletedCount)
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

  private String makeFile(String uploadPath, MultipartFile multipartFile) throws IOException {
    String fileId = UUID.randomUUID().toString();
    byte[] encrypted = fileEncryptor.encrypt(multipartFile.getBytes());
    Path path = Paths.get(uploadPath, fileId);
    Files.write(path, encrypted);

    return fileId;
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
      File file = FileUtils.getFile(fileDto.getFilePath(), fileDto.getFileId());

      deleteFile(file);
    }
  }

  private void deleteFile(File file) throws IOException {
    FileUtils.delete(file);
    deleteDirectory(file.getParentFile());
  }

  private void deleteDirectory(File directory) throws IOException {
    if (ObjectUtils.isEmpty(directory.list())) {
      FileUtils.deleteDirectory(directory);

      File parentDirectory = directory.getParentFile();
      if (!parentDirectory.toPath().endsWith(fileProperties.getBasePath())) {
        deleteDirectory(parentDirectory);
      }
    }
  }

  @Scheduled(cron = "0 0 4 * * *")
  public void deleteTempFile() throws IOException {
    File tempDirectory = new File(fileProperties.getTempPath());
    if (!tempDirectory.exists()) {
      return;
    }

    Instant currentTime = Instant.now();

    for (File serviceDirectory : Objects.requireNonNull(tempDirectory.listFiles())) {
      if (!serviceDirectory.exists()) {
        break;
      }

      for (File file : Objects.requireNonNull(serviceDirectory.listFiles())) {
        BasicFileAttributes fileAttributes = Files.readAttributes(file.toPath(),
            BasicFileAttributes.class);
        FileTime creationTime = fileAttributes.creationTime();
        Duration duration = Duration.between(creationTime.toInstant(), currentTime);

        if (duration.toDays() > fileProperties.getTempFileMaxStorageDays()) {
          deleteFile(file);
        }
      }
    }
  }

}
