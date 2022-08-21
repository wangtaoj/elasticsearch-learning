package com.wangtao.msgsearch.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wangtao
 * Created at 2022/8/21 17:27
 */
@Getter
@Setter
public class TradeVO {

    /**
     * 交易编号
     */
    private String tradeNo;

    /**
     * 成交金额
     */
    private Integer matchAmt;

    /**
     * 成交数量
     */
    private Integer matchQty;

}
