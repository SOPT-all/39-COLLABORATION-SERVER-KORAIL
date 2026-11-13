package org.sopt.korail.reservation.code;

import org.sopt.korail.global.swagger.ErrorCodeMarker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ErrorCodeMarker
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiReservationErrors {
    ReservationErrorCode[] value();
}
