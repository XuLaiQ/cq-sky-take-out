package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author  ：xulai
 * @File    ：== AutoFill.py ==
 * @Date    ：2024/7/13 20:58
 * @Describe:
 **/

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    /**
     * 操作类型：插入、更新
     */
    OperationType value();
}
