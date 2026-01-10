package com.vanmo.booking_app_be.exception;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatusCode())
                .body(ApiResponse.fail(ex.getMessage(), 401));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidate(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation error");
        return ResponseEntity.badRequest().body(ApiResponse.fail(msg, 402));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleOther(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("Unexpected error: " + ex.getMessage(), 403));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        ApiResponse<Void> res = ApiResponse.fail(
                "File quá lớn. Vui lòng upload file nhỏ hơn.",
                413
        );
        return ResponseEntity.status(413).body(res);
    }
}
