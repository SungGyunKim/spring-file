package com.spring.file.properties;

import com.spring.file.util.DefaultUtils;
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

  public FileProperties(String basePath, String tempFolder) {
    this.basePath = DefaultUtils.getValue(basePath, Paths.get("file").toAbsolutePath().toString());
    this.tempFolder = DefaultUtils.getValue(tempFolder, "temp");
  }

  public String getTempPath() {
    return basePath + File.separator + tempFolder;
  }

}
