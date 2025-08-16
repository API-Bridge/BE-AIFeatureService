// ## package: 이 클래스 파일이 속한 패키지 경로를 선언합니다. 테스트 소스 폴더(`src/test/java`) 내에 위치합니다.
package org.example.AIsvc.config;

// ## import: 필요한 Spring Security 및 Spring Framework 클래스들을 가져옵니다.
import org.springframework.boot.test.context.TestConfiguration; // ## 이 클래스가 테스트 전용 설정 클래스임을 나타냅니다.
import org.springframework.context.annotation.Bean; // ## 이 메소드가 Spring Bean을 생성함을 나타냅니다.
import org.springframework.context.annotation.Primary; // ## 같은 타입의 Bean이 여러 개 있을 때 우선적으로 사용할 Bean을 지정합니다.
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // ## HTTP 기반의 보안 설정을 구성하기 위한 클래스입니다.
import org.springframework.security.web.SecurityFilterChain; // ## Spring Security의 보안 필터 체인을 정의하는 인터페이스입니다.

// ## @TestConfiguration: 이 클래스는 오직 테스트 환경에서만 사용될 설정 파일임을 Spring에게 알려줍니다.
// ### 일반적인 @Configuration과 달리, 테스트 시에만 로드됩니다.
@TestConfiguration
public class TestSecurityConfig {

    // ## @Bean: 이 메소드가 반환하는 SecurityFilterChain 객체를 Spring 컨테이너의 Bean으로 등록합니다.
    // ## @Primary: 같은 타입의 Bean이 여러 개 있을 때 이 Bean을 우선적으로 사용하도록 지정합니다.
    // ### 이 Bean은 실제 애플리케이션의 SecurityConfig에 있는 SecurityFilterChain Bean을 대체(override)하게 됩니다.
    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        // ## http.authorizeHttpRequests(...): 모든 HTTP 요청에 대한 인가(Authorization) 규칙을 설정합니다.
        http.authorizeHttpRequests(auth -> auth
                // ## .anyRequest().permitAll(): 어떤 종류의 요청이든 인증 절차 없이 전부 허용합니다.
                .anyRequest().permitAll()
        );
        // ## http.csrf(...): CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화합니다.
        // ### 테스트 환경에서는 보통 이 기능을 비활성화하여 테스트의 복잡도를 낮춥니다.
        http.csrf(csrf -> csrf.disable());

        // ## 설정이 완료된 HttpSecurity 객체를 빌드하여 SecurityFilterChain 객체로 반환합니다.
        return http.build();
    }
}