-- =====================================================================
-- 초기 데이터 (ddl-auto: create 이후 서버 기동 시마다 실행됨)
-- 테이블이 매번 새로 만들어지므로 중복 걱정 없이 INSERT 한다.
-- MySQL 전용 문법(CURDATE, DATE_ADD 등)을 사용하므로 테스트(H2)에서는 실행하지 않는다.
-- =====================================================================

-- 회원: 예약 API는 Member-Id 헤더로 회원을 식별한다. (다른 회원의 예약 조회 확인용으로 2명)
INSERT INTO `member` (id) VALUES (1), (2);

-- 역
INSERT INTO station (name, is_major) VALUES
    ('서울', true),
    ('부산', true);

-- 열차 (하행 홀수, 상행 짝수)
INSERT INTO train (train_number, train_type) VALUES
    ('101', 'KTX'),
    ('103', 'KTX'),
    ('105', 'KTX'),
    ('102', 'KTX'),
    ('104', 'KTX'),
    ('106', 'KTX'),
    ('1001', 'ITX'),
    ('1002', 'ITX'),
    ('1101', 'ITX_MAEUM'),
    ('1102', 'ITX_MAEUM'),
    ('1201', 'MUGUNGHWA'),
    ('1202', 'MUGUNGHWA');

-- 호차: 모두 일반실. KTX 9량, ITX-새마을·ITX-마음·무궁화 6량
INSERT INTO coach (train_id, coach_number, seat_class)
SELECT t.id,
       n.n,
       'STANDARD'
FROM train t
JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) n
WHERE t.train_type = 'KTX' OR n.n <= 6
ORDER BY t.id, n.n;

-- 좌석: 일반실 14행 x A~D (56석)
--       KTX는 앞쪽 절반 순방향, 뒤쪽 절반 역방향 / 그 외 열차는 모두 순방향
INSERT INTO seat (coach_id, direction, seat_row, seat_col)
SELECT c.id,
       CASE WHEN t.train_type = 'KTX' AND r.n > 7 THEN 'BACKWARD' ELSE 'FORWARD' END,
       r.n,
       col.v
FROM coach c
JOIN train t ON t.id = c.train_id
JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
      UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14) r
JOIN (SELECT 'A' AS v UNION ALL SELECT 'B' UNION ALL SELECT 'C' UNION ALL SELECT 'D') col
WHERE c.seat_class = 'STANDARD'
ORDER BY c.id, r.n, col.v;

-- 운행 일정: 오늘(KST)부터 7일간 매일 같은 시간표
-- (서버 기동 시점 기준 날짜로 생성되므로 항상 미래 일정이 존재함)
-- MySQL 서버 시간대(UTC 등)와 무관하게 KST 날짜를 쓰기 위해 오프셋으로 변환한다. (시간대 테이블 없이 동작)
INSERT INTO schedule (departure_station_id, arrival_station_id, train_id, departure_at, arrival_at)
SELECT ds.id,
       ars.id,
       t.id,
       TIMESTAMP(DATE_ADD(DATE(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00')), INTERVAL d.n DAY), tpl.dep_time),
       TIMESTAMP(DATE_ADD(DATE(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00')), INTERVAL d.n DAY), tpl.arr_time)
FROM (SELECT '101' AS train_number, '서울' AS dep_station, '부산' AS arr_station, '06:00:00' AS dep_time, '08:38:00' AS arr_time
      UNION ALL SELECT '1001', '서울', '부산', '07:30:00', '12:20:00'
      UNION ALL SELECT '103',  '서울', '부산', '09:00:00', '11:40:00'
      UNION ALL SELECT '1201', '서울', '부산', '10:00:00', '15:40:00'
      UNION ALL SELECT '1101', '서울', '부산', '11:30:00', '16:10:00'
      UNION ALL SELECT '105',  '서울', '부산', '14:00:00', '16:40:00'
      UNION ALL SELECT '102',  '부산', '서울', '07:00:00', '09:40:00'
      UNION ALL SELECT '1002', '부산', '서울', '09:00:00', '13:50:00'
      UNION ALL SELECT '104',  '부산', '서울', '12:00:00', '14:40:00'
      UNION ALL SELECT '1202', '부산', '서울', '13:00:00', '18:40:00'
      UNION ALL SELECT '1102', '부산', '서울', '15:00:00', '19:40:00'
      UNION ALL SELECT '106',  '부산', '서울', '18:00:00', '20:40:00') tpl
JOIN train t ON t.train_number = tpl.train_number
JOIN station ds ON ds.name = tpl.dep_station
JOIN station ars ON ars.name = tpl.arr_station
JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
      UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) d
ORDER BY d.n, tpl.dep_time;

-- 운임: 구간 양방향 동일 요금, 어린이는 어른 요금의 50% (100원 단위 절사)
-- 호차가 있는 열차 종류·좌석 등급 조합마다 운임이 있어야 한다. (없으면 예약 시 FARE5001)
INSERT INTO fare (departure_station_id, arrival_station_id, train_type, seat_class, price, passenger_type)
SELECT s1.id,
       s2.id,
       f.train_type,
       f.seat_class,
       CASE WHEN p.passenger_type = 'ADULT' THEN f.adult_price ELSE FLOOR(f.adult_price / 200) * 100 END,
       p.passenger_type
FROM (SELECT '서울' AS station_a, '부산' AS station_b, 'KTX' AS train_type, 'STANDARD' AS seat_class, 59800 AS adult_price
      UNION ALL SELECT '서울', '부산', 'ITX',       'STANDARD', 42600
      UNION ALL SELECT '서울', '부산', 'ITX_MAEUM', 'STANDARD', 42600
      UNION ALL SELECT '서울', '부산', 'MUGUNGHWA', 'STANDARD', 28600) f
JOIN (SELECT 0 AS reversed UNION ALL SELECT 1) dir
JOIN station s1 ON s1.name = CASE WHEN dir.reversed = 0 THEN f.station_a ELSE f.station_b END
JOIN station s2 ON s2.name = CASE WHEN dir.reversed = 0 THEN f.station_b ELSE f.station_a END
JOIN (SELECT 'ADULT' AS passenger_type UNION ALL SELECT 'CHILD') p;

-- 예약: 모든 KTX 일정의 일반실 1~9호차 잔여석을 17, 6, 0, 13, 5, 0, 0, 3, 0석으로 맞춘다. (호차당 56석)
-- 결제 완료(PAID) 예약이라 결제 기한과 상관없이 점유로 계산된다. 예약자는 2번 회원이다.
INSERT INTO reservation (schedule_id, member_id, status, total_price, expires_at, created_at, updated_at)
SELECT s.id, 2, 'PAID', 0, s.departure_at, NOW(), NOW()
FROM schedule s
JOIN train t ON t.id = s.train_id
WHERE t.train_type = 'KTX'
ORDER BY s.id;

-- 티켓: 호차별 점유 좌석 수만큼. 빈 좌석이 한쪽에 몰리지 않도록 좌석 순서를 섞어서 고른다.
INSERT INTO ticket (reservation_id, seat_id, passenger_type, price)
SELECT r.id, x.seat_id, 'ADULT', 59800
FROM reservation r
JOIN schedule s ON s.id = r.schedule_id
JOIN (SELECT st.id AS seat_id,
             c.train_id,
             occ.occupied,
             ROW_NUMBER() OVER (PARTITION BY c.id
                                ORDER BY MOD(st.seat_row * 5 + ASCII(st.seat_col), 7), st.seat_row, st.seat_col) AS seq
      FROM seat st
      JOIN coach c ON c.id = st.coach_id
      JOIN train t ON t.id = c.train_id AND t.train_type = 'KTX'
      JOIN (SELECT 1 AS coach_number, 39 AS occupied
            UNION ALL SELECT 2, 50
            UNION ALL SELECT 3, 56
            UNION ALL SELECT 4, 43
            UNION ALL SELECT 5, 51
            UNION ALL SELECT 6, 56
            UNION ALL SELECT 7, 56
            UNION ALL SELECT 8, 53
            UNION ALL SELECT 9, 56) occ ON occ.coach_number = c.coach_number) x
  ON x.train_id = s.train_id AND x.seq <= x.occupied
WHERE r.member_id = 2;

UPDATE reservation r
SET r.total_price = (SELECT COUNT(*) * 59800 FROM ticket t WHERE t.reservation_id = r.id)
WHERE r.member_id = 2;
