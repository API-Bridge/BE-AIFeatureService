# 1단계: 빌드 환경
# FROM ... AS builder: 멀티-스테이지 빌드를 시작합니다. 'builder' 스테이지는 Java 코드를 컴파일하고
# JAR 파일을 빌드하는 데만 사용되는 임시 환경이며, 전체 JDK를 포함
FROM amazoncorretto:17-alpine AS builder

# 이후 모든 명령어의 기본 디렉토리를 설정
WORKDIR /app

# COPY: 로컬 머신의 Gradle 래퍼 파일들을 컨테이너로 복사
# 이를 통해 컨테이너가 프로젝트와 동일한 버전의 Gradle을 사용
COPY gradlew .
COPY gradle gradle

# 프로젝트의 의존성 및 설정 파일을 복사
COPY build.gradle settings.gradle ./

# 명령어 실행. 이 단계는 프로젝트의 모든 의존성을 다운로드
# 소스 코드를 복사하기 전에 이 단계를 실행하면, 의존성이 변경되지 않았을 경우 Docker가 이 레이어를 캐싱하여
# 다음 빌드 속도를 크게 향상
# --no-daemon: CI 환경에서 더 안정적인 빌드를 위해 Gradle 데몬을 사용하지 않음
RUN ./gradlew dependencies --no-daemon

# COPY: 애플리케이션의 소스 코드를 컨테이너로 복사
COPY src src

# 소스 코드를 컴파일하고, 테스트를 실행하며, 애플리케이션을 실행 가능한 JAR 파일로 패키징
# bootJar는 실행 가능한 JAR를 만들기 위해 사용
# -x test: 이 플래그는 Docker 빌드 중에는 테스트를 건너뛰도록 함
# 테스트는 보통 Jenkins 같은 CI 단계에서 이미 실행되었어야 합니다.
RUN ./gradlew bootJar --no-daemon -x test


# 2단계: 실행 환경
# FROM: 최종 이미지를 위한 새로운 베이스 이미지에서 시작. 애플리케이션을 실행하는 데는
# 전체 JDK가 필요 없으므로, 더 가벼운 JRE(Java Runtime Environment) 이미지를 사용하여
# 최종 이미지 크기를 줄임
FROM amazoncorretto:17-alpine

# RUN addgroup/adduser: 보안을 위해 root가 아닌 'spring'이라는 전용 사용자를 생성
# root가 아닌 사용자로 애플리케이션을 실행하는 것은 컨테이너 보안의 매우 중요
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

# 실행 컨테이너의 작업 디렉토리를 설정
WORKDIR /app

# 아래 HEALTHCHECK 명령어에 필요한 'curl' 패키지를 설치
RUN apk add --no-cache curl

# 'builder' 스테이지에서 빌드된 JAR 파일'만' 최종 이미지로 복사
# 이렇게 하면 소스 코드나 빌드 도구가 포함되지 않아 최종 이미지가 작게 유지
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 JAR 파일의 소유권을 우리가 생성한 'spring' 사용자로 변경
RUN chown spring:spring app.jar

# 이후 모든 명령어에 대한 활성 사용자를 root가 아닌 'spring' 사용자로 전환
USER spring:spring

# HEALTHCHECK: Docker나 쿠버네티스가 이 애플리케이션이 건강한지 확인하는 방법을 정의
# 30초마다 actuator health 엔드포인트에 'curl' 요청
# 명령이 실패하면 컨테이너는 'unhealthy' 상태로 표시
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Docker에게 컨테이너가 런타임에 지정된 네트워크 포트를 사용한다고 알림
EXPOSE 8080

# 컨테이너 환경에서 실행하기 위한 최적화된 JVM 옵션을 설정하여 메모리 사용량과 성능을 개선
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:G1HeapRegionSize=16m \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom"

# 컨테이너가 시작될 때 실행되는 메인 명령어
# "sh -c"를 사용하여 JAVA_OPTS 환경 변수가 JAR 파일 실행 전에 올바르게 적용
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]