package com.spring.file.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileSaveResponseDto {

  private List<FileDto> insertedList;

  private List<FileDto> deletedList;

  private List<FileDto> maintainedList;

}
