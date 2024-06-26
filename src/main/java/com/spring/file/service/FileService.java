package com.spring.file.service;

import com.spring.file.mapper.FileMapper;
import com.spring.file.model.FileCopiedDto;
import com.spring.file.model.FileCopyByServiceRequestDto;
import com.spring.file.model.FileCopyByServiceResponseDto;
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;
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

  public FileUploadResponseDto upload(FileUploadRequestDto requestDto) throws Exception {
    String tempPath = getServiceTempPath(requestDto.getServiceCode());
    List<FileUploadedDto> fileUploadedList = new ArrayList<>();

    for (MultipartFile multipartFile : requestDto.getFiles()) {
      fileUploadedList.add(FileUploadedDto.builder()
          .fileId(makeFile(tempPath, multipartFile))
          .fileName(FilenameUtils.removeExtension(multipartFile.getOriginalFilename()))
          .fileExtension(FilenameUtils.getExtension(multipartFile.getOriginalFilename()))
          .fileSize(multipartFile.getSize())
          .build());
    }

    return FileUploadResponseDto.builder()
        .files(fileUploadedList)
        .build();
  }

  @Transactional
  public FileSaveResponseDto save(FileSaveRequestDto requestDto) throws IOException {
    String tempPath = getServiceTempPath(requestDto.getServiceCode());
    String savePath = getServiceSavePath(requestDto.getServiceCode());

    FileDto params = FileDto.builder()
        .serviceCode(requestDto.getServiceCode())
        .tableName(requestDto.getTableName())
        .distinguishColumnValue(requestDto.getDistinguishColumnValue())
        .build();
    List<FileDto> savedFileList = fileMapper.findByService(params);

    // target
    List<FileDto> insertFileList = requestDto.getFiles().stream()
        .filter(file -> savedFileList.stream()
            .noneMatch(savedFile -> file.getFileId().equals(savedFile.getFileId()))
        )
        .map(file -> FileDto.builder()
            .fileId(file.getFileId())
            .filePath(savePath)
            .fileName(file.getFileName())
            .fileExtension(file.getFileExtension())
            .fileSize(file.getFileSize())
            .serviceCode(requestDto.getServiceCode())
            .tableName(requestDto.getTableName())
            .distinguishColumnValue(requestDto.getDistinguishColumnValue())
            .build())
        .toList();
    List<FileDto> deleteFileList = savedFileList.stream()
        .filter(savedFile -> requestDto.getFiles().stream()
            .noneMatch(file -> savedFile.getFileId().equals(file.getFileId()))
        ).toList();
    List<FileDto> maintainedList = savedFileList.stream()
        .filter(savedFile -> requestDto.getFiles().stream()
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
    deleteFiles(deleteFileList);

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
  public FileDeleteByFileIdsResponseDto deleteByFileIds(FileDeleteByFileIdsRequestDto requestDto)
      throws IOException {
    int deletedCount = 0;
    List<FileDto> fileDtoList = fileMapper.findByFileIds(requestDto.getFileIds());

    if (!ObjectUtils.isEmpty(fileDtoList)) {
      deletedCount = fileMapper.deleteByFileIds(requestDto.getFileIds());
      deleteFiles(fileDtoList);
    }

    return FileDeleteByFileIdsResponseDto.builder()
        .count(deletedCount)
        .build();
  }


  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public FileFindByServiceResponseDto findByService(FileFindByServiceRequestDto requestDto) {
    FileDto params = FileDto.builder()
        .serviceCode(requestDto.getServiceCode())
        .tableName(requestDto.getTableName())
        .distinguishColumnValue(requestDto.getDistinguishColumnValue())
        .build();
    List<FileDto> result = fileMapper.findByService(params);

    return FileFindByServiceResponseDto.builder()
        .files(result)
        .build();
  }

  @Transactional
  public FileDeleteByServiceResponseDto deleteByService(FileDeleteByServiceRequestDto requestDto)
      throws IOException {
    int deletedCount = 0;

    FileDto params = FileDto.builder()
        .serviceCode(requestDto.getServiceCode())
        .tableName(requestDto.getTableName())
        .distinguishColumnValue(requestDto.getDistinguishColumnValue())
        .build();
    List<FileDto> fileDtoList = fileMapper.findByService(params);

    if (!ObjectUtils.isEmpty(fileDtoList)) {
      deletedCount = fileMapper.deleteByService(params);
      deleteFiles(fileDtoList);
    }

    return FileDeleteByServiceResponseDto.builder()
        .count(deletedCount)
        .build();
  }

  @Transactional
  public FileCopyByServiceResponseDto copyByService(FileCopyByServiceRequestDto requestDto)
      throws IOException {
    String tempPath = getServiceTempPath(requestDto.getServiceCode());

    FileDto params = FileDto.builder()
        .serviceCode(requestDto.getServiceCode())
        .tableName(requestDto.getTableName())
        .distinguishColumnValue(requestDto.getDistinguishColumnValue())
        .build();
    List<FileDto> savedFileList = fileMapper.findByService(params);

    List<FileCopiedDto> copidFileList = new ArrayList<>();
    for (FileDto savedFile : savedFileList) {
      String fileId = getNewFileId();
      File srcFile = new File(savedFile.getFilePath(), savedFile.getFileId());
      File destFile = new File(tempPath, fileId);

      FileUtils.copyFile(srcFile, destFile);
      copidFileList.add(FileCopiedDto.builder()
          .fileId(fileId)
          .fileName(savedFile.getFileName())
          .fileExtension(savedFile.getFileExtension())
          .fileSize(savedFile.getFileSize())
          .build());
    }

    return FileCopyByServiceResponseDto.builder()
        .copiedList(copidFileList)
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
    String fileId = getNewFileId();
    byte[] encrypted = fileEncryptor.encrypt(multipartFile.getBytes());
    Path path = Paths.get(uploadPath, fileId);
    Files.createDirectories(path.getParent());
    Files.write(path, encrypted);

    return fileId;
  }

  private String getNewFileId() {
    return UUID.randomUUID().toString();
  }

  public Resource getResource(FileDto fileDto) throws Exception {
    Path path = Paths.get(fileDto.getFilePath(), fileDto.getFileId());
    byte[] decrypted = fileEncryptor.decrypt(Files.readAllBytes(path));
    Resource resource = new ByteArrayResource(decrypted);

    if (!resource.exists()) {
      throw new FileNotFoundException();
    } else if (!resource.isReadable()) {
      throw new Exception("파일을 읽을 수 없습니다.");
    }

    return resource;
  }

  public Resource getResizeResource(FileDto fileDto, int resizeWidth, int resizeHeight)
      throws Exception {
    Path path = Paths.get(fileDto.getFilePath(), fileDto.getFileId());
    byte[] decrypted = fileEncryptor.decrypt(Files.readAllBytes(path));
    BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(decrypted));
    Method scalingMethod = Method.AUTOMATIC;
    // {@link Mode.AUTOMATIC} : width, height를 참고하여 가장 적합한 크기로 이미지를 resize한다.
    // {@link Mode.FIT_EXACT} : width, height를 기준으로 이미지를 resize한다.
    // {@link Mode.FIT_TO_WIDTH} : width를 기준으로 이미지를 resize한다.
    // {@link Mode.FIT_TO_HEIGHT} : height를 기준으로 이미지를 resize한다.
    Mode resizeMode = Mode.AUTOMATIC;
    BufferedImage resizedImage = Scalr.resize(originalImage, scalingMethod, resizeMode, resizeWidth,
        resizeHeight);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(resizedImage, fileDto.getFileExtension(), baos);

    return new ByteArrayResource(baos.toByteArray());
  }

  private void deleteFiles(List<FileDto> fileDtoList) throws IOException {
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
    if (!ObjectUtils.isEmpty(directory.list())) {
      return;
    }

    FileUtils.deleteDirectory(directory);

    File parentDirectory = directory.getParentFile();
    if (!parentDirectory.toPath().endsWith(fileProperties.getBasePath())) {
      deleteDirectory(parentDirectory);
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
