package com.petrecovery.module.verify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petrecovery.module.verify.entity.VerifyCode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VerifyCodeMapper extends BaseMapper<VerifyCode> {
}
