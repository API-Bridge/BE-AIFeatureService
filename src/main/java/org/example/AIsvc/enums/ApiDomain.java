// ## package: 이 ENUM 파일이 속한 패키지 경로를 선언합니다.
package org.example.AIsvc.enums;

public enum ApiDomain {
    // 금융 도메인
    FINANCE("finance", "금융", "주식, 환율, 암호화폐, 금리 등 금융 관련 데이터"),
    // 날씨/환경 도메인
    WEATHER("weather", "날씨", "기상정보, 미세먼지, 환경 데이터"),
    // 뉴스/미디어 도메인
    NEWS("news", "뉴스", "뉴스, 소셜미디어, 트렌드 정보"),
    // 교통/위치 도메인
    TRANSPORTATION("transportation", "교통", "대중교통, 교통정보, 지도, 위치 서비스"),
    // 쇼핑/이커머스 도메인
    COMMERCE("commerce", "쇼핑", "상품정보, 가격비교, 리뷰, 쿠폰"),
    // 정부/공공 도메인
    GOVERNMENT("government", "정부", "정부 공공데이터, 통계청, 법령 정보"),
    // 엔터테인먼트 도메인
    ENTERTAINMENT("entertainment", "엔터테인먼트", "영화, 음악, 게임, 방송 정보"),
    // 스포츠 도메인
    SPORTS("sports", "스포츠", "경기결과, 선수정보, 리그 데이터"),
    // 헬스케어 도메인
    HEALTHCARE("healthcare", "헬스케어", "건강정보, 병원, 의료 데이터"),
    // 교육 도메인
    EDUCATION("education", "교육", "학습자료, 강의, 시험정보"),
    // 부동산 도메인
    REALESTATE("realestate", "부동산", "매매, 전세, 월세, 시세 정보"),
    // 여행 도메인
    TRAVEL("travel", "여행", "항공, 숙박, 관광지, 맛집 정보"),
    // 기술/IT 도메인
    TECHNOLOGY("technology", "기술", "개발자 정보, 기술 트렌드, API 문서"),
    // 라이프스타일 도메인
    LIFESTYLE("lifestyle", "라이프스타일", "패션, 뷰티, 인테리어, 요리"),
    // 기타 도메인
    OTHERS("others", "기타", "기타 분류되지 않은 데이터");

    private final String code; // API 통신이나 DB 저장 시 사용할 영문 코드 (e.g., "weather")
    private final String displayName; // 사용자에게 보여줄 한글 이름 (e.g., "날씨")
    private final String description; // 부가 설명

    ApiDomain(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCode() { return code; }

    public static ApiDomain fromCode(String code) {
        for (ApiDomain domain : values()) {
            if (domain.code.equalsIgnoreCase(code)) { // 대소문자 구분 없이 비교
                return domain;
            }
        }
        return OTHERS;
    }
}