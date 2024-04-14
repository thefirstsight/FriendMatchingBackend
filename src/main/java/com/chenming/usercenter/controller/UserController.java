package com.chenming.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenming.usercenter.common.BaseResponse;
import com.chenming.usercenter.common.ErrorCode;
import com.chenming.usercenter.common.ResultUtils;
import com.chenming.usercenter.exception.BusinessException;
import com.chenming.usercenter.model.domain.User;
import com.chenming.usercenter.model.request.UserLoginRequest;
import com.chenming.usercenter.model.request.UserRegisterRequest;
import com.chenming.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import static com.chenming.usercenter.constant.userConstant.ADMIN_ROLE;
import static com.chenming.usercenter.constant.userConstant.USER_LOGIN_STATE;


/**
 * 用户接口
 *
 * @author chenming
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:4000", allowCredentials = "true")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate redisTemplate;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
//            return ResultUtils.error(ErrorCode.PARAM_ERROR);
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        String userPassword = userRegisterRequest.getUserPassword();
        String userAccount = userRegisterRequest.getUserAccount();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
//            return ResultUtils.error(ErrorCode.PARAM_ERROR);
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
//        return new BaseResponse<>(0, result, "ok");
        return ResultUtils.success(result);
    }


    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return null;
        }
        String userPassword = userLoginRequest.getUserPassword();
        String userAccount = userLoginRequest.getUserAccount();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        User user = userService.userLogin(userAccount, userPassword, request);
//        return new BaseResponse<>(0, user, "ok");
        return ResultUtils.success(user);
    }


    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!isAdmin(request)) {
//            return new ArrayList<>();
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> {
            user.setUserPassword(null);
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null)
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        long userId = currentUser.getId();
        //从数据库中搜
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    @GetMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList){
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
          return ResultUtils.success(userList);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request){
        //校验参数是否为空
        if(user == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User loginUser = userService.getCurrentUser(request);
        int result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 推荐页面
     * 数据太多需要分页
     * @param request
     * @return
     */
    @Deprecated
    @GetMapping("/recommendAll")
    public BaseResponse<List<User>> recommendUsersOnce(HttpServletRequest request){
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 推荐页面
     * @param pageSize
     * @param pageNum
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum,HttpServletRequest request){
        User currentUser = userService.getCurrentUser(request);
        String redisKey = String.format("yupao:user:recommend:%s", currentUser.getId());
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        //如果有缓存，直接读缓存
        Page<User> userPage = (Page<User>)valueOperations.get(redisKey);
        if(userPage != null){
            return ResultUtils.success(userPage);
        }
        //无缓存，直接查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        try{
            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            log.error("redis set key error", e);
        }
        return ResultUtils.success(userPage);
    }

    private boolean isAdmin(HttpServletRequest request) {
        //仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

}
