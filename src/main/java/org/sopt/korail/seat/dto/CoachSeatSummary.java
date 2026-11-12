package org.sopt.korail.seat.dto;

// 호차별 전체, 점유 좌석 수 집계 결과
public record CoachSeatSummary(
        Long coachId,
        int coachNumber,
        long totalSeatCount,
        long occupiedSeatCount
) {
}
