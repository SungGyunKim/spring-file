package com.spring.file.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileCopiedDto {

  String fileId;

  String fileName;

  String fileExtension;

  long fileSize;

}
