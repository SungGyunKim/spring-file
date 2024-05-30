package com.spring.file.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileSaveRequestDto {

  @Size(min = 1)
  @NotBlank
  String serviceCode;

  @Size(max = 100)
  @NotBlank
  String tableName;

  @Size(max = 1000)
  @NotBlank
  String distinguishColumnValue;

  @Size(min = 1)
  @NotNull
  List<FileSaveDto> files;

}
