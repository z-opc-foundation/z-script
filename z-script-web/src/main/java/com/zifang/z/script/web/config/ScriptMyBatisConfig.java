package com.zifang.z.script.web.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
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
        // MyBatis-Plus 分页拦截器 —— **TASK-20260925-035 修的就是缺这一段**。
        //
        // 缺它时的症状（实测，2026-10-22）：
        //   · `BaseMapper.page(new Page<>(current, size), wrapper)` 不分页，**恒返回全表**
        //     （size=2 与 size=5 都是 131 行）；
        //   · count 从来没跑 ⇒ `page.getTotal()` 恒 0 ⇒ 响应里的 `total` 是 "0"。
        // 根因：本仓**自己**用 `MybatisSqlSessionFactoryBean` 装配 SqlSessionFactory，
        // 而 MP 的 `MybatisPlusAutoConfiguration` 在 application.yml 里被显式 exclude
        // （见 z-script-admin/src/main/resources/application.yml 的 autoconfigure.exclude，
        //  理由是"classpath 上再多一个自带工厂的模块就会以 expected single matching bean 崩启动"）
        // ⇒ 自动配置那条路上的分页插件从来不会进场，只能在这里手工挂。
        //
        // 刻意**不**把它声明成 Bean：主壳（z-opc main-starter）把 z-script-web 合进同一个 JVM，
        // 那里可能还有别的 MybatisPlusInterceptor Bean；按类型注入会让本方法在启动期
        // 撞上 "expected single matching bean but found 2"。直接在方法内 new，最省事也最安全。
        //
        // DbType 显式给 MYSQL：3.5.7 的 PaginationInnerInterceptor 没有无参构造，
        // 而本模块的数据源由 ModuleDataSourceTemplate 按 `z.base.db.script.*` 建连（组织标准库 = MySQL）。
        MybatisPlusInterceptor mpInterceptor = new MybatisPlusInterceptor();
        mpInterceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        factoryBean.setPlugins(mpInterceptor);
        // 不设置 mapperLocations：core/domain/mapper 下全部是 MP 的 BaseMapper + 注解 SQL，
        // 仓内没有任何 mapper/*.xml。留着 classpath*:/mapper/**/*.xml 只会让每次启动都 WARN
        // "Property 'mapperLocations' was specified but matching resources are not found"。
        factoryBean.setTypeAliasesPackage("com.zifang.z.script.core.domain.entity");
        return factoryBean.getObject();
    }
}
