package com.chenming.usercenter.service;

import com.chenming.usercenter.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chenming.usercenter.model.domain.User;

/**
* @author chenming
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-04-12 12:56:22
*/
public interface TeamService extends IService<Team> {
    long addTeam(Team team, User loginUser);
}
