package com.spring.file.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.UUID;

@Getter
@Setter
@Builder
public class FileDto {

  @UUID
  @Size(max = 50)
  @NotBlank
  String fileId;

  @Size(max = 500)
  @NotBlank
  String filePath;

  @Size(max = 255)
  @NotBlank
  String fileName;

  @Size(max = 100)
  @NotBlank
  String fileExtension;

  @Min(1)
  long fileSize;

  @Size(max = 20)
  @NotBlank
  String serviceCode;

  @Size(max = 100)
  @NotBlank
  String tableName;

  @Size(max = 1000)
  @NotBlank
  String distinguishColumnValue;

  public String getFileNameExtension() {
    return String.join(".", fileName, fileExtension);
  }

}
