package com.wangtao.msgsearch.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangtao
 * Created at 2022/8/21 17:22
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Msg {

    /**
     * 0001: 交易所报文
     * 0002: 银行间报文
     * 0003: 场外报文
     * es类型: keyword
     */
    private String type;

    /**
     * 流水号
     * es类型: keyword
     */
    private String traceNo;

    /**
     * 报文内容
     * es类型: text
     */
    private String content;

}
