package com.tianji.api.dto.remark;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class LikeTimesDTO {
    private Long bizId;
    private Integer likeTimes;


}
