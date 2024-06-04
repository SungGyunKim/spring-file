package com.spring.file.mapper;

import com.spring.file.model.FileDto;
import com.spring.file.model.FileInsertBulkDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper {

  int insertBulk(FileInsertBulkDto dto);

  FileDto findByFileId(String fileId);

}
