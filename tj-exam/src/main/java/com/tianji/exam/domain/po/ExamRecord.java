package com.tianji.exam.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.index.Indexed;


import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(value = "exam_records") // 1. 指定集合名(表名)
public class ExamRecord {
    @Id
    private String id;

    @Indexed
    private Long userId;

    //1-练习(Practice), 2-正式考试(Exam)
    private Integer type;
    private Long courseId;  // 课程ID
    private Long sectionId; // 小节ID (考试对应的节)

    private LocalDateTime createTime; // 开始考试时间
    private LocalDateTime submitTime; // 交卷时间
    private Double score;             // 得分
    private Boolean finished;         // 是否完成

    private List<ExamAnswer> answers;

}