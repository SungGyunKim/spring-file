package com.spring.file.config;

import com.spring.file.properties.FileProperties;
import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class FileConfig {

  private final FileProperties fileProperties;

  @Bean
  public StandardPBEByteEncryptor fileEncryptor() {
    StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();
    encryptor.setPassword(fileProperties.getEncryptorPassword());
    encryptor.setAlgorithm("PBEWithMD5AndTripleDES");

    return encryptor;
  }

}
