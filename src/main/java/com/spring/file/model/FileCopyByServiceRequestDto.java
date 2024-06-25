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
public class FileCopyByServiceRequestDto {

  @Size(max = 10)
  @NotBlank
  String serviceCode;

  @Size(max = 100)
  @NotBlank
  String tableName;

  @Size(max = 1000)
  @NotBlank
  String distinguishColumnValue;

}
