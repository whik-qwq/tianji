package com.tianji.remark.service;

import com.tianji.remark.domin.dto.LikeRecordFormDTO;
import com.tianji.remark.domin.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
* @author xuhe8
* @description 针对表【liked_record(点赞记录表)】的数据库操作Service
* @createDate 2026-02-12 21:38:34
*/
public interface LikedRecordService extends IService<LikedRecord> {

    void addLikeRecord(@Valid LikeRecordFormDTO likeRecordFormDTO);

    Set<Long> isBizLiked(List<Long> bizIds);

    void readLikedTimesAndSendMessage(String bizType, int maxBizSize);
}
