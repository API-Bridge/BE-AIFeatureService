package org.example.AIsvc.enums;

public enum ApiKeyword {
    // === 금융 관련 키워드 ===
    STOCK_PRICE("stock_price", "주가", ApiDomain.FINANCE, "개별 주식 가격 정보"),
    STOCK_INDEX("stock_index", "주가지수", ApiDomain.FINANCE, "코스피, 나스닥 등 지수"),
    EXCHANGE_RATE("exchange_rate", "환율", ApiDomain.FINANCE, "통화 환율 정보"),
    CRYPTOCURRENCY("cryptocurrency", "암호화폐", ApiDomain.FINANCE, "비트코인, 이더리움 등"),
    INTEREST_RATE("interest_rate", "금리", ApiDomain.FINANCE, "기준금리, 예적금 금리"),
    ECONOMIC_INDICATOR("economic_indicator", "경제지표", ApiDomain.FINANCE, "GDP, 물가지수 등"),

    // === 날씨 관련 키워드 ===
    CURRENT_WEATHER("current_weather", "현재날씨", ApiDomain.WEATHER, "실시간 날씨 정보"),
    WEATHER_FORECAST("weather_forecast", "날씨예보", ApiDomain.WEATHER, "미래 날씨 예측"),
    AIR_QUALITY("air_quality", "대기질", ApiDomain.WEATHER, "미세먼지, 공기질 지수"),
    TEMPERATURE("temperature", "기온", ApiDomain.WEATHER, "온도 정보"),
    PRECIPITATION("precipitation", "강수량", ApiDomain.WEATHER, "비, 눈 정보"),
    UV_INDEX("uv_index", "자외선지수", ApiDomain.WEATHER, "자외선 강도"),

    // === 뉴스 관련 키워드 ===
    BREAKING_NEWS("breaking_news", "속보", ApiDomain.NEWS, "실시간 속보"),
    POLITICS("politics", "정치", ApiDomain.NEWS, "정치 관련 뉴스"),
    ECONOMY_NEWS("economy_news", "경제뉴스", ApiDomain.NEWS, "경제 관련 뉴스"),
    TECHNOLOGY_NEWS("tech_news", "기술뉴스", ApiDomain.NEWS, "IT, 기술 관련 뉴스"),
    SPORTS_NEWS("sports_news", "스포츠뉴스", ApiDomain.NEWS, "스포츠 관련 뉴스"),
    SOCIAL_TREND("social_trend", "소셜트렌드", ApiDomain.NEWS, "SNS 트렌드, 이슈"),

    // === 교통 관련 키워드 ===
    SUBWAY_INFO("subway_info", "지하철정보", ApiDomain.TRANSPORTATION, "지하철 노선, 시간표"),
    BUS_INFO("bus_info", "버스정보", ApiDomain.TRANSPORTATION, "버스 위치, 도착시간"),
    TRAFFIC_CONDITION("traffic_condition", "교통상황", ApiDomain.TRANSPORTATION, "교통체증, 사고정보"),
    PARKING_INFO("parking_info", "주차정보", ApiDomain.TRANSPORTATION, "주차장 위치, 요금"),
    TAXI_FARE("taxi_fare", "택시요금", ApiDomain.TRANSPORTATION, "택시 요금 정보"),

    // === 쇼핑 관련 키워드 ===
    PRODUCT_INFO("product_info", "상품정보", ApiDomain.COMMERCE, "상품 상세 정보"),
    PRICE_COMPARISON("price_comparison", "가격비교", ApiDomain.COMMERCE, "쇼핑몰별 가격 비교"),
    PRODUCT_REVIEW("product_review", "상품리뷰", ApiDomain.COMMERCE, "사용자 리뷰, 평점"),
    COUPON_DISCOUNT("coupon_discount", "쿠폰할인", ApiDomain.COMMERCE, "할인 쿠폰 정보"),
    SHOPPING_RANK("shopping_rank", "쇼핑순위", ApiDomain.COMMERCE, "인기상품 랭킹"),

    // === 정부/공공 관련 키워드 ===
    PUBLIC_DATA("public_data", "공공데이터", ApiDomain.GOVERNMENT, "정부 공개 데이터"),
    LEGAL_INFO("legal_info", "법령정보", ApiDomain.GOVERNMENT, "법률, 규정 정보"),
    STATISTICS("statistics", "통계", ApiDomain.GOVERNMENT, "인구, 경제 통계"),
    PUBLIC_SERVICE("public_service", "공공서비스", ApiDomain.GOVERNMENT, "민원, 공공 서비스"),
    POLICY_INFO("policy_info", "정책정보", ApiDomain.GOVERNMENT, "정부 정책 발표"),

    // === 엔터테인먼트 관련 키워드 ===
    MOVIE_INFO("movie_info", "영화정보", ApiDomain.ENTERTAINMENT, "영화 정보, 상영시간"),
    MUSIC_CHART("music_chart", "음악차트", ApiDomain.ENTERTAINMENT, "음원 차트, 순위"),
    TV_SCHEDULE("tv_schedule", "방송편성", ApiDomain.ENTERTAINMENT, "TV 프로그램 편성표"),
    CELEBRITY_NEWS("celebrity_news", "연예뉴스", ApiDomain.ENTERTAINMENT, "연예인 관련 소식"),
    GAME_INFO("game_info", "게임정보", ApiDomain.ENTERTAINMENT, "게임 순위, 리뷰"),

    // === 스포츠 관련 키워드 ===
    SOCCER_RESULT("soccer_result", "축구결과", ApiDomain.SPORTS, "축구 경기 결과"),
    BASEBALL_RESULT("baseball_result", "야구결과", ApiDomain.SPORTS, "야구 경기 결과"),
    BASKETBALL_RESULT("basketball_result", "농구결과", ApiDomain.SPORTS, "농구 경기 결과"),
    SPORTS_SCHEDULE("sports_schedule", "경기일정", ApiDomain.SPORTS, "경기 일정 정보"),
    PLAYER_STATS("player_stats", "선수통계", ApiDomain.SPORTS, "선수 개인 기록"),
    TEAM_RANKING("team_ranking", "팀순위", ApiDomain.SPORTS, "리그 순위표"),

    // === 헬스케어 관련 키워드 ===
    HOSPITAL_INFO("hospital_info", "병원정보", ApiDomain.HEALTHCARE, "병원 위치, 진료과목"),
    HEALTH_TIP("health_tip", "건강정보", ApiDomain.HEALTHCARE, "건강 관리 정보"),
    MEDICINE_INFO("medicine_info", "의약품정보", ApiDomain.HEALTHCARE, "의약품 정보"),
    FITNESS_DATA("fitness_data", "운동정보", ApiDomain.HEALTHCARE, "운동, 다이어트 정보"),

    // === 교육 관련 키워드 ===
    EXAM_SCHEDULE("exam_schedule", "시험일정", ApiDomain.EDUCATION, "각종 시험 일정"),
    COURSE_INFO("course_info", "강의정보", ApiDomain.EDUCATION, "온라인 강의 정보"),
    SCHOLARSHIP("scholarship", "장학금", ApiDomain.EDUCATION, "장학금 정보"),
    SCHOOL_INFO("school_info", "학교정보", ApiDomain.EDUCATION, "학교 정보, 입학"),

    // === 부동산 관련 키워드 ===
    HOUSE_PRICE("house_price", "주택가격", ApiDomain.REALESTATE, "아파트, 주택 매매가"),
    RENT_INFO("rent_info", "임대정보", ApiDomain.REALESTATE, "전세, 월세 정보"),
    REAL_ESTATE_TREND("real_estate_trend", "부동산동향", ApiDomain.REALESTATE, "부동산 시장 동향"),

    // === 여행 관련 키워드 ===
    FLIGHT_INFO("flight_info", "항공정보", ApiDomain.TRAVEL, "항공편 정보, 가격"),
    HOTEL_INFO("hotel_info", "숙박정보", ApiDomain.TRAVEL, "호텔, 숙소 정보"),
    TOURIST_SPOT("tourist_spot", "관광지", ApiDomain.TRAVEL, "관광지 정보, 리뷰"),
    RESTAURANT_INFO("restaurant_info", "맛집정보", ApiDomain.TRAVEL, "맛집 추천, 리뷰"),

    // === 기술 관련 키워드 ===
    API_DOCUMENT("api_document", "API문서", ApiDomain.TECHNOLOGY, "API 사용법, 문서"),
    TECH_TREND("tech_trend", "기술동향", ApiDomain.TECHNOLOGY, "기술 트렌드, 뉴스"),
    DEVELOPER_TOOL("developer_tool", "개발도구", ApiDomain.TECHNOLOGY, "개발 도구, 라이브러리"),

    // === 라이프스타일 관련 키워드 ===
    FASHION_TREND("fashion_trend", "패션트렌드", ApiDomain.LIFESTYLE, "패션 트렌드 정보"),
    BEAUTY_TIP("beauty_tip", "뷰티정보", ApiDomain.LIFESTYLE, "화장품, 뷰티 팁"),
    RECIPE("recipe", "레시피", ApiDomain.LIFESTYLE, "요리 레시피"),
    INTERIOR_TIP("interior_tip", "인테리어", ApiDomain.LIFESTYLE, "인테리어 정보");

    private final String code; // 영문 코드
    private final String displayName; // 한글 이름
    private final ApiDomain domain; // 이 키워드가 속한 상위 도메인
    private final String description; // 부가 설명

    ApiKeyword(String code, String displayName, ApiDomain domain, String description) {
        this.code = code;
        this.displayName = displayName;
        this.domain = domain;
        this.description = description;
    }

    public String getCode() { return code; }
    public ApiDomain getDomain() { return domain; }

    public static ApiKeyword fromCode(String code) {
        for (ApiKeyword keyword : values()) {
            if (keyword.code.equalsIgnoreCase(code)) {
                return keyword;
            }
        }
        return null;
    }
}