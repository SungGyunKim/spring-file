package com.spring.file.mapper;

import com.spring.file.model.FileDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper {

  int insertBulk(List<FileDto> list);

  FileDto findByFileId(String fileId);

  List<FileDto> findByFileIds(List<String> fileIds);

  int deleteByFileIds(List<String> fileIds);

  List<FileDto> findByService(FileDto dto);

  int deleteByService(FileDto dto);

}
