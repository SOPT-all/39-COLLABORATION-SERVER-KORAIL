package org.sopt.korail.schedule.code;

import org.sopt.korail.global.swagger.ErrorCodeMarker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ErrorCodeMarker
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiScheduleErrors {
    ScheduleErrorCode[] value();
}
