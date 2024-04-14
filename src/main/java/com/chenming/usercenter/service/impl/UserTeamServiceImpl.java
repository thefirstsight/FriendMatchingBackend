package com.chenming.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenming.usercenter.model.domain.UserTeam;
import com.chenming.usercenter.service.UserTeamService;
import com.chenming.usercenter.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author chenming
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-04-12 13:00:47
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




