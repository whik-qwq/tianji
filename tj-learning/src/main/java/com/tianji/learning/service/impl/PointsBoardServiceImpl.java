package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.PointsBoardSeasonService;
import com.tianji.learning.service.PointsBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author xuhe8
* @description 针对表【points_board(学霸天梯榜)】的数据库操作Service实现
* @createDate 2026-02-18 15:02:45
*/
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard>
    implements PointsBoardService{

    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;
    private final PointsBoardSeasonService seasonService;

    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        Long season = query.getSeason();
        boolean isCurrent = season == null || season == 0;
        LocalDateTime now = LocalDateTime.now();
        String key= RedisConstants.POINTS_BOARD_KEY_PREFIX+ DateUtils.POINTS_BOARD_SUFFIX_FORMATTER.format(now);
        PointsBoard myBoard = isCurrent ? queryMyCurrentBoard(key):queryMyHistoryBoard(season);
        List<PointsBoard> list =isCurrent ?
                queryMyCurrentBoardList(key,query.getPageNo(),query.getPageSize())
                :queryMyHistoryBoardList(query);
        PointsBoardVO vo = new PointsBoardVO();
        if(myBoard!=null){
            vo.setPoints(myBoard.getPoints());
            vo.setRank(myBoard.getRank());
        }
        if(CollUtils.isEmpty(list)){
            return vo;
        }
        Set<Long> uIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        Map<Long, String> userMap = new HashMap<>(uIds.size());
        if(CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        List<PointsBoardItemVO> items = new ArrayList<>(list.size());
        for (PointsBoard p : list) {
            PointsBoardItemVO v = new PointsBoardItemVO();
            v.setPoints(p.getPoints());
            v.setRank(p.getRank());
            v.setName(userMap.get(p.getUserId()));
            items.add(v);
        }
        vo.setBoardList(items);
        return vo;
    }

    @Override
    public void createPointsBoardTableBySeason(Integer season) {
        getBaseMapper().createPointsBoardTable("points_board_"+season);
    }

    private List<PointsBoard> queryMyHistoryBoardList(PointsBoardQuery query) {
        String tableName="points_board_"+query.getSeason();
        int from = (query.getPageNo()-1)*query.getPageSize();
        return getBaseMapper().queryMyHistoryBoardList(tableName,from,from + query.getPageSize() - 1);
    }
    @Override
    public List<PointsBoard> queryMyCurrentBoardList(String key, Integer pageNo, Integer pageSize) {
        int from = (pageNo-1)*pageSize;
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, from, from + pageSize - 1);
        if(CollUtils.isEmpty(tuples)){
            return CollUtils.emptyList();
        }
        int rank=from+1;
        List<PointsBoard> list =new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            PointsBoard board  = new PointsBoard();
            if(userId==null||points==null){
                continue;
            }
            board.setUserId(Long.valueOf(userId));
            board.setPoints(points.intValue());
            board.setRank(rank++);
            list.add(board);
        }
        return list;
    }
    private PointsBoard queryMyHistoryBoard(Long season) {
        Long userId = UserContext.getUser();
        String tableName="points_board_"+season;
        return getBaseMapper().queryMyHistoryBoard(tableName,userId);
    }

    private PointsBoard queryMyCurrentBoard(String key) {
        BoundZSetOperations<String, String> ops = redisTemplate.boundZSetOps(key);
        String userId = UserContext.getUser().toString();
        Double points = ops.score(userId);
        Long rank = ops.reverseRank(userId);
        PointsBoard myBoard = new PointsBoard();
        myBoard.setPoints(points ==null?0:points.intValue());
        myBoard.setRank(rank==null?0:rank.intValue()+1);
        return myBoard;
    }
}




