package com.chenming.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenming.usercenter.common.ErrorCode;
import com.chenming.usercenter.constant.TeamStatusEnum;
import com.chenming.usercenter.exception.BusinessException;
import com.chenming.usercenter.model.domain.Team;
import com.chenming.usercenter.model.domain.User;
import com.chenming.usercenter.model.domain.UserTeam;
import com.chenming.usercenter.service.TeamService;
import com.chenming.usercenter.mapper.TeamMapper;
import com.chenming.usercenter.service.UserService;
import com.chenming.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

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
}




