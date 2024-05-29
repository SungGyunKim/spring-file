package com.spring.file.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileSaveTempResponseDto {

  private List<FileDto> files;

}
