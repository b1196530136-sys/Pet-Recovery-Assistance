package com.petrecovery.module.archive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;

public interface ArchiveService extends IService<StrayAnimalArchive> {
    StrayAnimalArchive createArchive(StrayAnimalArchive archive, Long userId);
}
