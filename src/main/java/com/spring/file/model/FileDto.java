package com.spring.file.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class FileDto {

  String id;

  String name;

  String extension;

  long size;

}
