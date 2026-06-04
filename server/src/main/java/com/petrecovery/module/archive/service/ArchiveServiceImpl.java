package com.petrecovery.module.archive.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;
import com.petrecovery.module.archive.mapper.ArchiveMapper;
import org.springframework.stereotype.Service;

@Service
public class ArchiveServiceImpl extends ServiceImpl<ArchiveMapper, StrayAnimalArchive> implements ArchiveService {

    @Override
    public StrayAnimalArchive createArchive(StrayAnimalArchive archive, Long userId) {
        archive.setUserId(userId);
        archive.setStatus("PENDING");
        save(archive);
        return archive;
    }
}
