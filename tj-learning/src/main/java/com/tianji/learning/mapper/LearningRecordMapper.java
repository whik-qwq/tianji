package com.tianji.learning.mapper;

import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author xuhe8
* @description 针对表【learning_record(学习记录表)】的数据库操作Mapper
* @createDate 2026-02-02 10:27:13
* @Entity com.tianji.learning.domain.po.LearningRecord
*/
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {

    List<IdAndNumDTO> countLearnedSections(Long userId);
}




