package org.sopt.korail.reservation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.korail.reservation.code.ReservationSuccessCode;
import org.sopt.korail.reservation.dto.CreateReservationRequest;
import org.sopt.korail.reservation.dto.CreateReservationResponse;
import org.sopt.korail.reservation.dto.ReservationDetailResponse;
import org.sopt.korail.reservation.service.ReservationService;
import org.sopt.korail.reservation.swagger.ApiReservationSuccess;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @ApiReservationSuccess(ReservationSuccessCode.CREATE_RESERVATION)
    public CreateReservationResponse createReservation(
            @Valid @RequestBody CreateReservationRequest request
    ) {
        return reservationService.createReservation(request);
    }

    @GetMapping("/{reservationId}")
    @ApiReservationSuccess(ReservationSuccessCode.GET_RESERVATION)
    public ReservationDetailResponse getReservation(@PathVariable Long reservationId) {
        return reservationService.getReservation(reservationId);
    }
}
