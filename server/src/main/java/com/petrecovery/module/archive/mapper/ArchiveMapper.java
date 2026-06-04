package com.petrecovery.module.archive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArchiveMapper extends BaseMapper<StrayAnimalArchive> {
}
