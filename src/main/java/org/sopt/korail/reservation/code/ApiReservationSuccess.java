package org.sopt.korail.reservation.code;

import org.sopt.korail.global.swagger.SuccessCodeMarker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SuccessCodeMarker
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiReservationSuccess {
    ReservationSuccessCode value();
}
