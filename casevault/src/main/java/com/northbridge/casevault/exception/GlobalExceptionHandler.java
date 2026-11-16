package com.northbridge.casevault.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Too-large uploads are stopped before our own code runs, so they need their own handler
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> tooLarge() {
        return ResponseEntity.status(413).body("File is too large. The limit is 2 MB.");
    }
}