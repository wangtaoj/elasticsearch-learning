package com.wangtao.es.api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author wangtao
 * Created at 2022/8/21 15:04
 */
@NoArgsConstructor
@ToString
@Setter
@Getter
@Accessors(chain = true)
public class User {

    private Integer userId;

    private String name;

    private Integer age;

    private LocalDate birthday;

    private LocalDateTime createTime;
}
