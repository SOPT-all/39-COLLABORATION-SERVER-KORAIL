package org.sopt.korail.seat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.korail.seat.domain.Direction;
import org.sopt.korail.seat.domain.Seat;

public record SeatResponse(
        @Schema(description = "좌석 ID", example = "101")
        Long seatId,

        @Schema(description = "좌석 번호 (행 + 열)", example = "1A")
        String seatNumber,

        @Schema(description = "행", example = "1")
        int row,

        @Schema(description = "열", example = "A")
        String column,

        @Schema(description = "좌석 방향", example = "FORWARD")
        Direction direction,

        @Schema(description = "선택 가능 여부 (빈 좌석이면 true)", example = "true")
        boolean available
) {
    public static SeatResponse of(Seat seat, boolean available) {
        return new SeatResponse(
                seat.getId(),
                seat.getSeatRow() + seat.getSeatCol(),
                seat.getSeatRow(),
                seat.getSeatCol(),
                seat.getDirection(),
                available
        );
    }
}
