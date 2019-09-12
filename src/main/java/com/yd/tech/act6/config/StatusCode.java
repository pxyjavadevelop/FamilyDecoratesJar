package com.yd.tech.act6.config;

/**
 * 状态码
 * @author pxy
 */
public enum StatusCode {

    Success(1,"成功"),
    Fail(0,"失败"),
    NotFound(10010,"不存在"),
    Entity_Not_Exist(10011,"实例信息不存在"),
    Invalid_Params(10012,"请求参数不合法"),
    Special(2,"系统错误，请联系管理员");

    StatusCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private Integer code;
    private String msg;

    public Integer getCode() {
        return code;
    }
    public void setCode(Integer code) {
        this.code = code;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }

}
