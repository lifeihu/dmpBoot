package com.sw.bi.scheduler.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Aspect
@Configuration
public class HibernateConfig {

    Logger log = LoggerFactory.getLogger(getClass());

    public String current_session_context_class = "org.springframework.orm.hibernate5.SpringSessionContext";

    @Autowired
    private DataSource dataSource;

    private static final String AOP_POINTCUT_EXPRESSION = "execution (* com.sw.bi.scheduler.service..*.*(..))";




    private static final int TX_METHOD_TIMEOUT = 60;


    @Bean(name = "schedulerSessionFactory")
    public LocalSessionFactoryBean sessionFactoryBean() {
        log.info("执行sessionFactoryBean()");
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);
        try {
            sessionFactoryBean.setMappingLocations(new PathMatchingResourcePatternResolver().getResources(
                    "classpath:mapper/*.hbm.xml"));// dao和entity的公共包
        } catch (IOException e) {
            e.printStackTrace();
        }
        Properties properties = new Properties();


// 	properties.setProperty("hibernate.username", "root");
// 	properties.setProperty("hibernate.password", "root");
// 	properties.setProperty("hibernate.url", "jdbc:mysql://localhost:3306/hibernate?serverTimezone=UTC");
// 	properties.setProperty("hibernate.driver_class", "com.mysql.cj.jdbc.Driver");
//
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.GoldilocksDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.current_session_context_class", current_session_context_class);
        sessionFactoryBean.setHibernateProperties(properties);

        return sessionFactoryBean;
    }

    @Bean
    public HibernateTransactionManager transactionManager() {
        log.info("执行transactionManager()");
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setDataSource(dataSource);
        transactionManager.setSessionFactory(sessionFactoryBean().getObject()); // 注入sessionFactory
        transactionManager.setHibernateManagedSession(false); // 获取当前session
//        transactionManager.setValidateExistingTransaction(true); // 开启事务校验
        transactionManager.setRollbackOnCommitFailure(true);
        transactionManager.setAutodetectDataSource(true);
        transactionManager.setGlobalRollbackOnParticipationFailure(true);

        return transactionManager;
    }

    @Bean
    public TransactionInterceptor txAdvice()  {
        /**
         * 这里配置只读事务
         */
        RuleBasedTransactionAttribute readOnlyTx = new RuleBasedTransactionAttribute();
        readOnlyTx.setReadOnly(true);
        readOnlyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        /**
         * 必须带事务
         * 当前存在事务就使用当前事务，当前不存在事务,就开启一个新的事务
         */
        RuleBasedTransactionAttribute requiredTx = new RuleBasedTransactionAttribute();
        //检查型异常也回滚
        requiredTx.setRollbackRules(
                Collections.singletonList(new RollbackRuleAttribute(Exception.class)));
        requiredTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        requiredTx.setTimeout(TX_METHOD_TIMEOUT);

        /***
         * 无事务地执行，挂起任何存在的事务
         */
        RuleBasedTransactionAttribute noTx = new RuleBasedTransactionAttribute();
        noTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

        Map<String, TransactionAttribute> txMap = new HashMap<>();
        //只读事务
        txMap.put("get*", readOnlyTx);
        txMap.put("query*", readOnlyTx);
        txMap.put("find*", readOnlyTx);
        txMap.put("list*", readOnlyTx);
        txMap.put("count*", readOnlyTx);
        txMap.put("exist*", readOnlyTx);
        txMap.put("search*", readOnlyTx);
        txMap.put("fetch*", readOnlyTx);
        txMap.put("pag*", readOnlyTx);
        //无事务
        txMap.put("noTx*", noTx);
        //写事务
        txMap.put("test*", readOnlyTx);
        txMap.put("add*", requiredTx);
        txMap.put("save*", requiredTx);
        txMap.put("insert*", requiredTx);
        txMap.put("update*", requiredTx);
        txMap.put("modify*", requiredTx);
        txMap.put("delete*", requiredTx);

        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
        source.setNameMap(txMap);

        return new TransactionInterceptor(transactionManager(), source);

    }
//
//    @Bean
//    public BeanNameAutoProxyCreator beanNameAutoProxyCreator() throws Exception {
//        BeanNameAutoProxyCreator beanNameAutoProxyCreator = new BeanNameAutoProxyCreator();
//        beanNameAutoProxyCreator.setBeanNames("*ServiceImpl");
//        beanNameAutoProxyCreator.setInterceptorNames("transactionInterceptor");
//
//        return beanNameAutoProxyCreator;
//    }


    @Bean
    public Advisor txAdviceAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(AOP_POINTCUT_EXPRESSION);
        return new DefaultPointcutAdvisor(pointcut, txAdvice());
    }

//
//    @Pointcut("execution (* com.sw.bi.scheduler.service..*.*(..)) ")
//    public void auth(){
//
//    }
//
//    //前置通知，切点之前执行
//    @Before("auth()")
//    public void deBefore(JoinPoint joinPoint) throws Throwable {
//        Object[] args = joinPoint.getArgs();
//       System.out.println(joinPoint.getTarget());
//    }


}