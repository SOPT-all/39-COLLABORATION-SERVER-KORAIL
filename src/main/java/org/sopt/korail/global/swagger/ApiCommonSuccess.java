package org.sopt.korail.global.swagger;

import org.sopt.korail.global.code.CommonSuccessCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SuccessCodeMarker
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiCommonSuccess {
    CommonSuccessCode value();
}

