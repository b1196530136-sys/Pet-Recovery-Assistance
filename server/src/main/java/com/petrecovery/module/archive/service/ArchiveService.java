package com.petrecovery.module.archive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;

public interface ArchiveService extends IService<StrayAnimalArchive> {
    StrayAnimalArchive createArchive(StrayAnimalArchive archive, Long userId);
    void updateArchive(StrayAnimalArchive archive, Long userId);
    void deleteArchive(Long id, Long userId);
    void approveArchive(Long id);
    void rejectArchive(Long id, String reason);
}
