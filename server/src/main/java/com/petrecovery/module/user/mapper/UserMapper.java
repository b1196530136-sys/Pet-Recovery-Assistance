package com.petrecovery.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petrecovery.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
}
