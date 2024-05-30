package com.spring.file.model;

import java.util.List;
import lombok.Builder;
import lombok.Singular;

@Builder
public class FileInsertBulkDto {

  @Singular
  List<FileDto> files;

}
