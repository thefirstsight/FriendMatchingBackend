package com.chenming.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenming.usercenter.common.BaseResponse;
import com.chenming.usercenter.common.ErrorCode;
import com.chenming.usercenter.common.ResultUtils;
import com.chenming.usercenter.exception.BusinessException;
import com.chenming.usercenter.model.domain.Team;
import com.chenming.usercenter.model.domain.User;
import com.chenming.usercenter.model.dto.TeamQuery;
import com.chenming.usercenter.model.request.TeamAddRequest;
import com.chenming.usercenter.model.request.UserLoginRequest;
import com.chenming.usercenter.model.request.UserRegisterRequest;
import com.chenming.usercenter.service.TeamService;
import com.chenming.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.chenming.usercenter.constant.userConstant.ADMIN_ROLE;
import static com.chenming.usercenter.constant.userConstant.USER_LOGIN_STATE;


/**
 * 用户接口
 *
 * @author chenming
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = "http://localhost:4000", allowCredentials = "true")
@Slf4j
public class TeamController {
    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User loginUser = userService.getCurrentUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);     //这里不懂
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        boolean result = teamService.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody Team team) {
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        boolean result = teamService.updateById(team);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(@RequestBody long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    //请求体中未包含数据，不需要@requestbody
    @GetMapping("/list")
    public BaseResponse<List<Team>> listTeams(TeamQuery teamQuery){
        if(teamQuery == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);     //根据team里设置的字段进行crud
        List<Team> teamList = teamService.list(queryWrapper);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamByPage(TeamQuery teamQuery){
        if(teamQuery == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }
}
