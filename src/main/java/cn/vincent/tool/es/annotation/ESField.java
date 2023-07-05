package cn.vincent.tool.es.annotation;

import cn.vincent.tool.es.enums.ESAnalyzerEnum;
import cn.vincent.tool.es.enums.ESTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES映射字段
 * 需要映射到ES中的字段 需添加此注释
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ESField {

    // 映射字段名称 默认取字段名称
    String fieldName() default "";

    // 映射字段类型
    ESTypeEnum type() default ESTypeEnum.KEYWORD;

    // 时间字段格式
    String format() default "";

    // 分词器
    ESAnalyzerEnum analyzer() default ESAnalyzerEnum.NULL;
}
