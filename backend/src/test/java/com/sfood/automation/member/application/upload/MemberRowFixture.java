package com.sfood.automation.member.application.upload;

import java.util.ArrayList;
import java.util.List;

public final class MemberRowFixture {

    private MemberRowFixture() {
    }

    /** LIST 43컬럼 행 (인덱스 0~42) */
    public static List<Object> listRow() {
        List<Object> row = new ArrayList<>();
        row.add(900101);                    // 0 회원번호
        row.add(45292.5);                   // 1 등록일 (시리얼 + 시간 분수부)
        row.add("2024-01-02 09:00:00");     // 2 가입승인일 (문자열 — 혼재 검증)
        row.add("승인");                     // 3 가입승인여부
        row.add("pc");                      // 4 가입경로
        row.add("2026-06-01 10:00:00");     // 5 최종로그인일
        row.add("business");                // 6 회원구분
        row.add(9);                         // 7 등급
        row.add("user1");                   // 8 아이디
        row.add("닉네임");                   // 9 닉네임
        row.add("홍길동");                   // 10 이름
        row.add("hong@example.com");        // 11 이메일
        row.add("010-1234-5678");           // 12 휴대폰번호
        row.add("02-123-4567");             // 13 전화번호
        row.add("06000 서울시 강남구 테헤란로 1"); // 14 주소
        row.add("y");                       // 15 메일수신여부
        row.add("n");                       // 16 SMS수신여부
        row.add(12500.0);                   // 17 주문금액
        row.add("1500점");                  // 18 마일리지
        row.add("0원");                     // 19 예치금
        row.add(5);                         // 20 방문횟수
        row.add("(주)테스트");               // 21 상호
        row.add("123-45-67890");            // 22 사업자번호
        row.add("김대표");                   // 23 대표자명
        row.add("도매");                     // 24 업태
        row.add("식품");                     // 25 종목
        row.add("06000 서울시 송파구 올림픽로 2"); // 26 사업자 주소
        row.add("02-000-0000");             // 27 팩스 번호
        row.add("recomm1");                 // 28 추천인아이디
        row.add("m");                       // 29 성별
        row.add("1990-01-01");              // 30 생일
        row.add("y");                       // 31 결혼여부
        row.add("2020-01-01");              // 32 결혼기념일
        row.add("01");                      // 33 직업
        row.add("food|etc");                // 34 관심분야
        row.add(1);                         // 35 개인정보유효기간
        row.add("메모");                     // 36 남기는말씀
        row.add("식품");                     // 37 추가정보1
        row.add("y");                       // 38 추가정보2
        row.add("홍대리");                   // 39 추가정보3
        row.add(null);                      // 40 추가정보4
        row.add(null);                      // 41 추가정보5
        row.add(null);                      // 42 추가정보6
        return row;
    }

    /** TEMPLATE 51컬럼 행 — 인덱스 0~7 에쓰푸드몰 전용 + 8~50 LIST와 동일 */
    public static List<Object> templateRow() {
        List<Object> row = new ArrayList<>();
        row.add("user1");          // 0 아이디 (중복)
        row.add("O");              // 1 거래처등록
        row.add(990954.0);         // 2 거래처코드
        row.add("(에쓰푸드몰)테스트"); // 3 거래처명
        row.add("(에쓰푸드몰)테스트"); // 4 거래처명 (중복)
        row.add("O");              // 5 세금계산서여부
        row.add("확인완료");        // 6 중복여부 확인
        row.add("license.jpg");    // 7 파일명
        row.addAll(listRow());     // 8~50 = LIST 0~42
        return row;
    }
}
