package com.spring.file.properties;

import com.spring.file.util.DefaultUtils;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.File;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ToString
@Getter
@Validated
@ConfigurationProperties("file")
public class FileProperties {

  @Size(min = 1)
  @NotBlank
  private final String basePath;

  @Size(min = 1)
  @NotBlank
  private final String tempFolder;

  @Min(1)
  private final Integer tempFileMaxStorageDays;

  @Size(min = 1)
  @NotBlank
  private final String saveFolder;

  @Size(min = 1)
  @NotBlank
  private final String encryptorPassword;

  public FileProperties(String basePath, String tempFolder, Integer tempFileMaxStorageDays,
      String saveFolder,
      String encryptorPassword) {
    this.basePath = DefaultUtils.getValue(basePath, Paths.get("file").toAbsolutePath().toString());
    this.tempFolder = DefaultUtils.getValue(tempFolder, "temp");
    this.tempFileMaxStorageDays = DefaultUtils.getValue(tempFileMaxStorageDays, 1);
    this.saveFolder = DefaultUtils.getValue(saveFolder, "save");
    this.encryptorPassword = DefaultUtils.getValue(encryptorPassword, "encryptorPassword");
  }

  public String getTempPath() {
    return basePath + File.separator + tempFolder;
  }

  public String getSavePath() {
    return basePath + File.separator + saveFolder;
  }

}
