package com.chenming.usercenter.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TeamJoinRequest implements Serializable {
    private static final long serialVersionUID = -3296064070254855834L;
    private Long teamId;
    private String password;
}
