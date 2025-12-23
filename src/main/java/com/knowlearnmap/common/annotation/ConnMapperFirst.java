package com.knowlearnmap.common.annotation;

import java.lang.annotation.*;

/**
 * First 데이터소스 MyBatis Mapper 표시 애노테이션
 * 
 * MybatisConfigFirst에서 이 애노테이션이 붙은 인터페이스를 스캔하여
 * 첫 번째 데이터베이스(knowlearn_map)에 연결합니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConnMapperFirst {
}
