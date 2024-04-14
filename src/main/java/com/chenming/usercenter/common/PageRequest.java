package com.chenming.usercenter.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 5009663700425141922L;
    /**
     * 页面大小
     */
    protected int pageSize;

    /**
     * 当前是第几页
     */
    protected int pageNum;

}