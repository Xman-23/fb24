# 1. JDK17 기반 이미지 사용
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# Gradle wrapper와 소스 코드 복사
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
COPY src/ src/

# Gradle로 빌드
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# 실제 런타임 이미지
FROM openjdk:17-jdk-slim

WORKDIR /app

# 빌드 결과 JAR 복사
COPY --from=builder /app/build/libs/portfolio-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]