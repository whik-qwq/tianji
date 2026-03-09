package com.tianji.remark.controller;

import com.tianji.remark.domin.dto.LikeRecordFormDTO;
import com.tianji.remark.domin.po.LikedRecord;
import com.tianji.remark.service.LikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
public class LikeRecordController {
    private final LikedRecordService likedRecordService;

    @PostMapping()
    public void addLikeRecord(@Valid @RequestBody LikeRecordFormDTO likeRecordFormDTO) {
        likedRecordService.addLikeRecord(likeRecordFormDTO);
    }

    @GetMapping("list")
    public Set<Long> isBizLiked(@RequestParam("bizIds") List<Long> bizIds){
        return likedRecordService.isBizLiked(bizIds);
    }
}
