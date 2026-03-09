package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CategoryBasicDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningNoteDTO;
import com.tianji.learning.domain.po.LearningNote;
import com.tianji.learning.domain.query.NoteAdminPageQuery;
import com.tianji.learning.domain.query.NotePageQuery;
import com.tianji.learning.domain.vo.LearningNoteVO;
import com.tianji.learning.service.LearningNoteService;
import com.tianji.learning.mapper.LearningNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
* @author xuhe8
* @description 针对表【learning_note(学习笔记表)】的数据库操作Service实现
* @createDate 2026-03-04 08:59:24
*/
@Service
@RequiredArgsConstructor
public class LearningNoteServiceImpl extends ServiceImpl<LearningNoteMapper, LearningNote>
    implements LearningNoteService{
    private final UserClient userClient;
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private  final CategoryClient categoryClient;

    @Override
    public void saveLearningNote(LearningNoteDTO dto) {
        LearningNote learningNote = BeanUtils.copyBean(dto, LearningNote.class);
        learningNote.setUserId(UserContext.getUser());
        learningNote.setAuthorId(UserContext.getUser());
        save(learningNote);
    }

    @Override
    public void gatherNote(Long id) {
        Long userId = UserContext.getUser();
        LearningNote note = getById(id);
        if (note == null) {
            throw new BizIllegalException("笔记不存在");
        }
        if (note.getAuthorId().equals(userId)) {
            throw new BizIllegalException("不能采集自己的笔记");
        }
        String gatherIdsStr = note.getGatherIds();
        List<String> idList = new ArrayList<>();
        if (gatherIdsStr != null && !gatherIdsStr.isEmpty()) {
            idList.addAll(Arrays.asList(gatherIdsStr.split(",")));
        }
        String userIdStr = String.valueOf(userId);
        // 1. 追加ID并更新原笔记
        idList.add(userIdStr);
        String newGatherIds = String.join(",", idList);
        note.setGatherIds(newGatherIds);
        note.setUsedTimes(note.getUsedTimes() + 1);
        updateById(note);

        // 2. 创建新笔记
        LearningNote myNewNote = new LearningNote();
        myNewNote.setUserId(userId);
        myNewNote.setCourseId(note.getCourseId());
        myNewNote.setChapterId(note.getChapterId());
        myNewNote.setSectionId(note.getSectionId());
        myNewNote.setNoteMoment(note.getNoteMoment());
        myNewNote.setContent(note.getContent());
        myNewNote.setIsPrivate(true);
        myNewNote.setIsGathered(true);
        myNewNote.setOriginNoteId(note.getId()); // 记录来源
        myNewNote.setAuthorId(note.getAuthorId());
        boolean success = save(myNewNote);
        if (!success) {
            throw new BizIllegalException("采集笔记失败");
        }
    }

    @Override
    public void updateNote(Long id, LearningNoteDTO dto) {
        Long userId = UserContext.getUser();
        LearningNote note = getById(id);
        if (note == null) {
            throw new BizIllegalException("笔记不存在");
        }
        if (!note.getUserId().equals(userId)) {
            throw new BizIllegalException("只能修改自己的笔记");
        }
        if (dto.getContent() == null || dto.getContent().isEmpty()) {
            throw new BizIllegalException("笔记内容不能为空");
        }
        note.setContent(dto.getContent());
        if (dto.getIsPrivate() != null){
            note.setIsPrivate(!dto.getIsPrivate());
        }
        boolean success = updateById(note);
        if (!success) {
            throw new BizIllegalException("更新笔记失败");
        }
    }

    public void deleteNote(Long id) {
        Long userId = UserContext.getUser();
        LearningNote note = getById(id);
        if (note == null) {
            throw new BizIllegalException("笔记不存在");
        }
        if (!note.getUserId().equals(userId)) {
            throw new BizIllegalException("只能删除自己的笔记");
        }
        boolean success = removeById(id);
        if (!success)
            throw new BizIllegalException("删除笔记失败");
    }

    public PageDTO<LearningNoteVO> queryNotePage(NotePageQuery query) {
        Long userId = UserContext.getUser();
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        Boolean onlyMine = query.getOnlyMine();
        Page<LearningNote> page = lambdaQuery()
                .eq(onlyMine, LearningNote::getUserId, userId)
                .eq(LearningNote::getHidden, false)
                .eq(!onlyMine, LearningNote::getIsGathered, false)
                .eq(courseId != null, LearningNote::getCourseId, courseId)
                .eq(sectionId != null, LearningNote::getSectionId, sectionId)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<LearningNote> records = page.getRecords();
        if (CollUtils.isEmpty(records)) return PageDTO.empty(page);
        Set<Long> userIds = records.stream().filter(r -> !r.getIsPrivate())
                .map(LearningNote::getUserId).collect(Collectors.toSet());
        Map<Long, UserDTO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserDTO> users = userClient.queryUserByIds(userIds); // 建议增加批量查询接口
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        List<LearningNoteVO> vos = new ArrayList<>();
        for (LearningNote record : records) {
            LearningNoteVO vo = BeanUtils.copyBean(record, LearningNoteVO.class);
            if (!vo.getIsPrivate()) {
                //如果不是匿名笔记
                UserDTO user = userMap.get(record.getUserId());
                vo.setAuthorIcon(user.getIcon());
                vo.setAuthorName(user.getUsername());
                vo.setAuthorId(user.getId());
            }
            vos.add(vo);
        }
        return PageDTO.of(page, vos);
    }

    @Override
    public PageDTO<LearningNoteVO> queryAdminNotePage(NoteAdminPageQuery query) {
        List<Long> queryCoursesId=null;
        if(query.getName()!=null){
            queryCoursesId = courseClient.queryCoursesIdByName(query.getName());
        }
        Page<LearningNote> page = lambdaQuery()
                .in(query.getName() != null, LearningNote::getCourseId, queryCoursesId)
                .eq(query.getHidden() != null, LearningNote::getHidden, query.getHidden())
                .ge(query.getBeginTime() != null, LearningNote::getCreateTime, query.getBeginTime())
                .le(query.getEndTime() != null, LearningNote::getCreateTime, query.getEndTime())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<LearningNote> records = page.getRecords();
        if (CollUtils.isEmpty(records)) return PageDTO.empty(page);
        ArrayList<LearningNoteVO> vos = new ArrayList<>();
        //去根据课程id拿课程名称
        List<Long> courseIds = records.stream()
                .map(LearningNote::getCourseId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, String> courseNameMap = courseClient.getSimpleInfoList(courseIds).stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, CourseSimpleInfoDTO::getName));

        List<Long> chapterIds = records.stream().map(LearningNote::getChapterId)
                .filter(Objects::nonNull).collect(Collectors.toList());

        List<Long> sectionIds = records.stream().map(LearningNote::getSectionId)
                .filter(Objects::nonNull).collect(Collectors.toList());

        Map<Long, String> cataNameMap = catalogueClient.batchQueryCatalogue(chapterIds).stream()
                .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));

        Map<Long, String> sectionNameMap = catalogueClient.batchQueryCatalogue(sectionIds).stream()
                .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));

        Set<Long> userIds = records.stream().map(LearningNote::getAuthorId).collect(Collectors.toSet());
        Map<Long, UserDTO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        for (LearningNote record : records) {
            LearningNoteVO vo = BeanUtils.copyBean(record, LearningNoteVO.class);
            //补充缺失的数据
            vo.setChapterName(cataNameMap.get(record.getChapterId()));
            vo.setCourseName(courseNameMap.get(record.getCourseId()));
            UserDTO user = userMap.get(record.getUserId());
            if (user != null) {
                vo.setAuthorName(user.getName());
            }
            vo.setSectionName(sectionNameMap.get(record.getSectionId()));
            vos.add(vo);
        }
        return PageDTO.of(page, vos);
    }

    public LearningNoteVO queryNoteDetail(Long id) {
        LearningNote note = getById(id);
        if (note == null) {
            throw new BizIllegalException("笔记不存在");
        }
        LearningNoteVO vo = BeanUtils.copyBean(note, LearningNoteVO.class);
        //补充缺失的数据
        Long courseId = note.getCourseId();
        Long chapterId = note.getChapterId();
        Long sectionId = note.getSectionId();
        if (courseId == null || chapterId == null || sectionId == null) throw new BizIllegalException("笔记信息不完整");

        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(courseId, true, false);
        //批量查询目录名称 (一次性传入所有需要查名称的ID，减少RPC次数)
        List<Long> cataIds = List.of(note.getChapterId(), note.getSectionId());
        Map<Long, String> cataNameMap = catalogueClient.batchQueryCatalogue(cataIds)
                .stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));

        List<CategoryBasicDTO> allOfOneLevel = categoryClient.getAllOfOneLevel();
        Set<Long> allUserIds = new HashSet<>();
        // 添加原作者ID
        if (note.getAuthorId() != null) {
            allUserIds.add(note.getAuthorId());
        }
        // 解析并添加采集者ID
        Set<Long> gatherIds = new HashSet<>();
        if (StringUtils.isNotBlank(note.getGatherIds())) {
            gatherIds = Arrays.stream(note.getGatherIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());
            allUserIds.addAll(gatherIds);
        }
        Map<Long, UserDTO> userMap = userClient.queryUserByIds(allUserIds)
                .stream()
                .collect(Collectors.toMap(UserDTO::getId, u -> u));
        //分类的map
        Map<Long, String> categoryMap = allOfOneLevel.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CategoryBasicDTO::getId, CategoryBasicDTO::getName));
        vo.setCourseName(courseInfo.getName());
        vo.setChapterName(cataNameMap.get(note.getChapterId()));
        vo.setSectionName(cataNameMap.get(note.getSectionId()));
        String firstCateName = categoryMap.get(courseInfo.getFirstCateId());
        String secondCateName = categoryMap.get(courseInfo.getSecondCateId());
        String thirdCateName = categoryMap.get(courseInfo.getThirdCateId());
        List<String> cateList = Stream.of(firstCateName, secondCateName, thirdCateName).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        vo.setCategoryNames(String.join("/", cateList));
        UserDTO author = userMap.get(note.getAuthorId());
        if (author != null) {
            vo.setAuthorName(author.getName());
            vo.setAuthorPhone(author.getCellPhone());
        }
        if (!gatherIds.isEmpty()){
            List<String> gatherNames = gatherIds.stream().map(c -> {
                UserDTO user = userMap.get(c);
                return user == null ? null : user.getName();
            }).collect(Collectors.toList());
            vo.setGathers(gatherNames);
        }
        return vo;
    }

    public void updateNoteHidden(Long id, Boolean hidden) {
        LearningNote note = getById(id);
        if (note == null) {
            throw new BizIllegalException("笔记不存在");
        }
        note.setHidden(hidden);
        boolean success = updateById(note);
        if (!success) {
            throw new BizIllegalException("修改笔记隐藏状态失败");
        }
    }
}




