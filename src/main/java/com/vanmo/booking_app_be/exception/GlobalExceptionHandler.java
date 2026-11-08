package com.vanmo.booking_app_be.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.vanmo.booking_app_be.dto.common.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getErrorCode().getHttpStatusCode())
                .body(ApiResponse.fail(ex.getMessage(), ex.getErrorCode().getCode()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(ErrorCode.UN_AUTHORIZED.getHttpStatusCode())
                .body(ApiResponse.fail(ErrorCode.UN_AUTHORIZED.getMessage(), ErrorCode.UN_AUTHORIZED.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidate(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation error");
        return ResponseEntity.badRequest().body(ApiResponse.fail(msg, 402));
    }

    /** A query or path parameter of the wrong type, e.g. an unknown enum value: the caller's mistake, not a 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        ex.getName() + ": giá trị không hợp lệ '" + ex.getValue() + "'",
                        ErrorCode.BAD_REQUEST.getCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleOther(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.internalServerError().body(ApiResponse.fail("Unexpected error: " + ex.getMessage(), 403));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        ApiResponse<Void> res = ApiResponse.fail("File quá lớn. Vui lòng upload file nhỏ hơn.", 413);
        return ResponseEntity.status(413).body(res);
    }
}
