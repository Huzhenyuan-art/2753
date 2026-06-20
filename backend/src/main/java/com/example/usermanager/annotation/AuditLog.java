package com.example.usermanager.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    String operation();

    String module() default "";

    String description() default "";

    boolean recordParams() default true;

    boolean recordResult() default false;
}
