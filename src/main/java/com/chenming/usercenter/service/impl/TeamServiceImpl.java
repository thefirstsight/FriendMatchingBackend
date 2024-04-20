package com.chenming.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenming.usercenter.common.ErrorCode;
import com.chenming.usercenter.constant.TeamStatusEnum;
import com.chenming.usercenter.exception.BusinessException;
import com.chenming.usercenter.model.domain.Team;
import com.chenming.usercenter.model.domain.User;
import com.chenming.usercenter.model.domain.UserTeam;
import com.chenming.usercenter.model.dto.TeamQuery;
import com.chenming.usercenter.model.request.TeamJoinRequest;
import com.chenming.usercenter.model.request.TeamUpdateRequest;
import com.chenming.usercenter.model.vo.TeamUserVO;
import com.chenming.usercenter.model.vo.UserVO;
import com.chenming.usercenter.service.TeamService;
import com.chenming.usercenter.mapper.TeamMapper;
import com.chenming.usercenter.service.UserService;
import com.chenming.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author chenming
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-04-12 12:56:22
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor =  Exception.class)
    public long addTeam(Team team, User loginUser) {
        //请求参数为空
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //是否登陆
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //校验信息
        //队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if(maxNum < 1 || maxNum > 20){
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍人数不满足要求");
        }
        //队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍标题不满足要求");
        }
        //描述 <= 512
        String description = team.getDescription();
        if(StringUtils.isNotBlank(description) && description.length() > 512){
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍描述过长");
        }
        //status 是否公开，不传默认为0
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍状态不满足要求");
        }
        //如果status是加密状态，一定要密码 且密码<=32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "密码设置不正确");
            }
        }
        //超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if(new Date().after(expireTime)){
            throw new BusinessException(ErrorCode.PARAM_ERROR, "超出时间 > 当前时间");
        }
        //用户最多可以创建5个队伍
        //todo 有bug。可能同时创建100个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        final long userId = loginUser.getId();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户最多创建5个队伍");
        }
        //插入队伍消息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || team == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "创建队伍失败");
        }
        //插入用户 ==> 队伍关系 到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if(id == null || id <= 0){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Team oldTeam = this.getById(id);
        if(oldTeam == null){
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        //只有管理员或者队伍创建者才可以修改
        if(oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //如果队伍状态改为加密，必须要有密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if(statusEnum.equals(TeamStatusEnum.SECRET)){
            if(StringUtils.isBlank(teamUpdateRequest.getPassword())){
                throw new BusinessException(ErrorCode.PARAM_ERROR, "加密房间必须设置密码");
            }
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if(teamJoinRequest == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        if(teamId == null || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Team team = this.getById(teamId);
        if(team == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍不存在");
        }
        Date expireTime = team.getExpireTime();
        if(expireTime != null && expireTime.before(new Date())){
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if(TeamStatusEnum.SECRET.equals(teamStatusEnum)){
            throw new BusinessException(ErrorCode.PARAM_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (teamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "密码错误");
            }
        }
        //检测用户加入队伍数是否超过5个
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
        if (hasJoinNum > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最多创建和加入5个队伍");
        }
        //不能重复加入已加入的队伍
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.eq("teamId", teamId);
        long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
        if (hasUserJoinTeam > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户已加入该队伍");
        }
        //已加入队伍的人数
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        long teamHasJoinNum = userTeamService.count(userTeamQueryWrapper);
        if (teamHasJoinNum >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍已满");
        }
        //修改队伍信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);

    }

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            //查询最大人数相等
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxMum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            //根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            //根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && !statusEnum.equals(TeamStatusEnum.PUBLIC)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        //不展示已过期的队伍
        queryWrapper.and(qw->qw.gt("expireTime", new Date()).or().isNull("expireTime"));

        List<Team> teamList = this.list(queryWrapper);
        if(CollectionUtils.isEmpty(teamList)){
            return new ArrayList<>();
        }
        //查询创建人的信息
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for(Team team:teamList){
            Long userId = team.getUserId();
            if(userId == null){
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);     //将team赋值给teamUserVO
            //脱敏用户信息
            if(user != null){
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }
}




