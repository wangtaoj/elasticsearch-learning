package com.wangtao.msgsearch.vo;

import lombok.Data;

/**
 * @author wangtao
 * Created at 2022/8/21 20:12
 */
@Data
public class MsgSearchVO {

    /**
     * 报文类型
     */
    private String type;

    /**
     * 流水号
     */
    private String traceNo;

    /**
     * 关键字, 从报文内容搜索
     */
    private String keyword;

    private Integer pageNo = 1;

    private Integer pageSize = 20;
}
