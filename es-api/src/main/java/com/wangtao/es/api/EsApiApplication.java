package com.wangtao.es.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author wangtao
 * Created at 2024-01-12
 */
@SpringBootApplication(scanBasePackages={"com.wangtao"})
public class EsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsApiApplication.class, args);
    }
}
