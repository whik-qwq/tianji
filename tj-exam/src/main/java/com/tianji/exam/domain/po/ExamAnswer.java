package com.tianji.exam.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(value = "exam_records")
public  class ExamAnswer {
    private Long questionId; // 题目ID
    private String answer;   // 用户填写的答案 (如 "A" 或 "A,B")
    private Boolean correct; // 是否答对
    private Integer score;   // 这道题得了多少分
}
