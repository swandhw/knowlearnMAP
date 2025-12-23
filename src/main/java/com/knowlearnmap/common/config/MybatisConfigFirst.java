package com.knowlearnmap.common.config;

import com.knowlearnmap.common.annotation.ConnMapperFirst;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * First MyBatis 설정
 * 
 * 주 데이터베이스(knowlearn_map) 연결 설정
 * - HikariCP 커넥션 풀 사용
 * - MyBatis 매퍼 스캔 (@ConnMapperFirst)
 * - 트랜잭션 관리 활성화
 */
@Slf4j
@Configuration
@MapperScan(
    value = "com.knowlearnmap", 
    annotationClass = ConnMapperFirst.class, 
    sqlSessionFactoryRef = "firstSqlSessionFactory"
)
@EnableTransactionManagement
public class MybatisConfigFirst {

    @Bean(name = "firstDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.first.datasource")
    public DataSource firstDataSource() {
        HikariDataSource hikari = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
        
        // HikariCP 최적화 설정
        hikari.setMaximumPoolSize(20);
        hikari.setMinimumIdle(5);
        hikari.setIdleTimeout(300000);  // 5분
        hikari.setMaxLifetime(1800000); // 30분
        hikari.setLeakDetectionThreshold(60000);
        hikari.setConnectionTimeout(20000);
        hikari.setAutoCommit(true);
        hikari.setValidationTimeout(5000);
        hikari.setConnectionTestQuery("SELECT 1");
        
        log.info("First DataSource 초기화 완료: {}", hikari.getJdbcUrl());
        return hikari;
    }

    @Bean(name = "firstSqlSessionFactory")
    @Primary
    public SqlSessionFactory firstSqlSessionFactory(
            @Qualifier("firstDataSource") DataSource firstDataSource,
            ApplicationContext applicationContext) throws Exception {

        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(firstDataSource);
        
        // MyBatis XML 매퍼 파일 위치 지정
        sqlSessionFactoryBean.setMapperLocations(
            applicationContext.getResources("classpath:mybatis-mapper/first/**/*.xml")
        );

        // MyBatis Configuration 설정
        org.apache.ibatis.session.Configuration configuration = 
            new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);  // snake_case -> camelCase 자동 변환
        sqlSessionFactoryBean.setConfiguration(configuration);

        log.info("First SqlSessionFactory 초기화 완료");
        return sqlSessionFactoryBean.getObject();
    }

    @Bean(name = "firstSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate firstSqlSessionTemplate(
            @Qualifier("firstSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
