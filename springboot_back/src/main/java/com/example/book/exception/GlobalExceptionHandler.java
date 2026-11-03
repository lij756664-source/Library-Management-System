package com.example.book.exception;

import com.example.book.common.BusinessException;
import com.example.book.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception e) {
        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().getFieldError() != null) {
            message = ex.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof BindException ex && ex.getBindingResult().getFieldError() != null) {
            message = ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        return Result.error(400, message);
    }

    // 请求参数在进入Controller前转换失败，属于输入错误，不应落入500兜底。
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        if (LocalDate.class.equals(e.getRequiredType())) {
            return Result.error(400, "日期格式不正确，请使用年-月-日，例如2026-11-03");
        }
        return Result.error(400, "请求参数格式不正确，请检查后重试");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeException(MaxUploadSizeExceededException e) {
        return Result.error(400, "上传文件大小不能超过2MB");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        return Result.error(404, "请求的资源不存在");
    }

    @ExceptionHandler({DataIntegrityViolationException.class, SQLIntegrityConstraintViolationException.class})
    public Result<Void> handleDataIntegrityException(Exception e) {
        String message = e.getMessage();
        if (message != null && message.contains("fk_borrow_book")) {
            return Result.error(400, "该图书存在借阅记录，无法删除");
        }
        if (message != null && message.contains("fk_book_category")) {
            return Result.error(400, "该分类下仍有图书，无法删除");
        }
        if (message != null && message.contains("uk_username")) {
            return Result.error(400, "用户名已存在");
        }
        return Result.error(400, "数据存在关联，无法删除");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
}
