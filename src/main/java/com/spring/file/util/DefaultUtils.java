package com.spring.file.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultUtils {

  public static <T> T getValue(T checkValue, T defaultValue) {
    return ObjectUtils.isEmpty(checkValue) ? defaultValue : checkValue;
  }

}
