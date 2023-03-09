package com.coldsky.vhr.utils;

import com.coldsky.vhr.model.Hr;
import org.springframework.security.core.context.SecurityContextHolder;


public class HrUtils {
    // 获取当前用户
    public static Hr getCurrentHr() {
        return ((Hr) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
