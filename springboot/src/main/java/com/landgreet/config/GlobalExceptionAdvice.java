package com.landgreet.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

@ControllerAdvice
public class GlobalExceptionAdvice {

    /** Tomcat rejects oversized uploads before our code runs; keep it friendly. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(HttpServletRequest request) {
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("error", "That image is too large — 5 MB max.");
        return "redirect:/account";
    }
}
