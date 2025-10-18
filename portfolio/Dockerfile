# 배포시 반드시 내 java 버전을 확인하여 버전 맞추기 (cmd -> - java version)
# 1단계: 빌드
FROM openjdk:22-jdk-slim AS builder
WORKDIR /app
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
COPY src/ src/
RUN chmod +x gradlew
RUN apt-get update && apt-get install -y dos2unix && rm -rf /var/lib/apt/lists/*
RUN dos2unix gradlew
RUN ./gradlew clean build -x test

# 2단계: 실행
FROM openjdk:22-jdk-slim
WORKDIR /app

# 업로드 이미지 폴더 미리 생성
RUN mkdir -p /app/upload/image

COPY --from=builder /app/build/libs/portfolio-0.0.1-SNAPSHOT.jar app.jar
# 3단계: Render 사이트의 DB환경변수 연결을 위한 배포용 application-prod.properties 설정 ("--spring.profiles.active=prod")
ENTRYPOINT ["sh", "-c", "java -Xmx128m -Xms64m -XX:+UseSerialGC -jar app.jar --spring.profiles.active=prod"]
