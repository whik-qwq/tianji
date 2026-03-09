package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @author xuhe8
* @description 针对表【interaction_question(互动提问的问题表)】的数据库操作Mapper
* @createDate 2026-02-08 19:37:52
* @Entity com.tianji.learning.domain.po.InteractionQuestion
*/
public interface InteractionQuestionMapper extends BaseMapper<InteractionQuestion> {
    @Select("UPDATE interaction_question SET hidden=#{hidden} where id =#{id} ")
    void hiddenQuestion(@Param("id")Long id,@Param("hidden") Boolean hidden);
}




