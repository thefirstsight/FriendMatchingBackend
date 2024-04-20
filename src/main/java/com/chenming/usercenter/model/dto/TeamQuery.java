package com.chenming.usercenter.model.dto;

import com.chenming.usercenter.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @EqualsAndHashCode(callSuper = true)
 * 不加该注解的影响：子类对象属性值一致，但其继承的父类对象属性值不一致，在比较的时候会出现比较结果不对的情况
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TeamQuery extends PageRequest {
    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 搜索关键词
     */
    private String searchText;
}
