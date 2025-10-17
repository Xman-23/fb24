FROM openjdk:17-jdk-slim AS builder
WORKDIR /app

# gradlew와 프로젝트 복사
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
COPY src/ src/

# 실행 권한 + line ending 문제 해결
RUN chmod +x gradlew
RUN apt-get update && apt-get install -y dos2unix && rm -rf /var/lib/apt/lists/*
RUN dos2unix gradlew

# Gradle wrapper 업데이트 & 빌드
RUN ./gradlew clean build -x test