package com.spring.file.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class FileCopyRequestDto {

  @Size(min = 1)
  @NotBlank
  String serviceCode;

  @Size(min = 1)
  @NotBlank
  @Size(max = 100)
  String tableName;

  @Size(min = 1)
  @NotBlank
  @Size(max = 1000)
  String distinguishColumnValue;

}