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
COPY --from=builder /app/build/libs/portfolio-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]