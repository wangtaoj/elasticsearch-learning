package com.wangtao.msgsearch.constant;

/**
 * @author wangtao
 * Created at 2022/8/21 17:34
 */
public enum MsgTypeEnum {

    JYS("0001", "交易所报文"),

    YHJ("0002", "银行间报文"),

    CW("0003", "场外报文");

    private final String value;

    private final String text;

    MsgTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static MsgTypeEnum ofOrdinal(int ordinal) {
        return values()[ordinal];
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
