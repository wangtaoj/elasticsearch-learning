package com.wangtao.msgsearch.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangtao
 * Created at 2022/8/21 15:02
 */
@RestController
public class ElasticsearchController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Elasticsearch!";
    }
}
