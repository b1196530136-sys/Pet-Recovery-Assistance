package com.petrecovery.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petrecovery.module.message.entity.SysImMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<SysImMessage> {
}
