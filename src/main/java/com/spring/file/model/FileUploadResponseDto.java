package com.spring.file.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder
public class FileUploadResponseDto {

  @Singular
  private List<FileUploadedDto> files;

}
