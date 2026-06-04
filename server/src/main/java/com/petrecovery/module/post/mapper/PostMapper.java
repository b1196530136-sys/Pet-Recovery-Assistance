package com.petrecovery.module.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petrecovery.module.post.entity.PetSearchPost;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<PetSearchPost> {
}
