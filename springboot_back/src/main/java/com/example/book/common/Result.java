package com.example.book.common;

/**
 * 统一返回结果封装
 * <p>code：200-成功，400-参数/业务错误，401-未登录或凭证无效，403-无权限，500-服务器错误</p>
 * <p>注意：本类不依赖 Lombok，getter/setter 全部手写，避免因注解处理器缺失导致的编译报错</p>
 */
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public Result() {
    }

    /** 操作成功（带数据） */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /** 操作成功（带提示信息，无数据） */
    public static Result<Void> success(String message) {
        Result<Void> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        return result;
    }

    /** 操作成功（自定义提示信息 + 数据） */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /** 操作失败（默认服务器错误） */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /** 操作失败（自定义错误码） */
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
