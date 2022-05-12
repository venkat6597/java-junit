package com.marksandspencer.foodshub.pal.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AspectLogger {
    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Before.
     *
     * @param joinPoint the join point
     */
    @Before("execution(* com.marksandspencer.foodshub.pal.service.*.*(..))")
    public void before(JoinPoint joinPoint){
        logger.debug("Before execution of {}", joinPoint);
    }

    /**
     * After.
     *
     * @param joinPoint the join point
     */
    @After(value = "execution(* com.marksandspencer.foodshub.pal.service.*.*(..))")
    public void after(JoinPoint joinPoint) {
        logger.debug("After execution of {}", joinPoint);
    }
}
