package com.spring.file.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileFindByServiceResponseDto {

  private List<FileDto> files;

}
