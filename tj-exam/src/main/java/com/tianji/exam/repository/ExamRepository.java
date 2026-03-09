package com.tianji.exam.repository;

import com.tianji.exam.domain.po.ExamRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExamRepository extends MongoRepository<ExamRecord, String> {

    ExamRecord findByUserIdAndSectionIdAndFinished(Long userId, Long sectionId, boolean b);

    boolean existsByUserIdAndSectionIdAndFinished(Long userId, Long sectionId, boolean b);
}