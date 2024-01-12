package com.wangtao.es.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author wangtao
 * Created at 2022/8/21 15:04
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {

    private Integer userId;

    private String name;

    private Integer age;

    private LocalDate birthday;

    private LocalDateTime createTime;
}
