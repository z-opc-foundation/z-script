package com.zifang.z.script.web.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.z.boot.datasource.starter.ModuleDataSourceTemplate;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.zifang.z.script.core.domain.mapper", sqlSessionFactoryRef = "sqlSessionFactoryScript")
public class ScriptMyBatisConfig extends ModuleDataSourceTemplate {

    @Bean("dataSourceScript")
    public DataSource dataSource(Environment env) {
        return buildDataSource(env, "script");
    }

    @Bean("sqlSessionFactoryScript")
    public SqlSessionFactory sqlSessionFactoryScript(DataSource dataSourceScript) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSourceScript);
        // 不设置 mapperLocations：core/domain/mapper 下全部是 MP 的 BaseMapper + 注解 SQL，
        // 仓内没有任何 mapper/*.xml。留着 classpath*:/mapper/**/*.xml 只会让每次启动都 WARN
        // "Property 'mapperLocations' was specified but matching resources are not found"。
        factoryBean.setTypeAliasesPackage("com.zifang.z.script.core.domain.entity");
        return factoryBean.getObject();
    }
}
