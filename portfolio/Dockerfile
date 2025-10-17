# 1. JDK 17 기반 이미지 사용
FROM openjdk:17-jdk-slim

# 2. JAR 파일을 컨테이너 내부로 복사
COPY build/libs/portfolio-0.0.1-SNAPSHOT.jar app.jar

# 3. 컨테이너가 실행될 때 JAR을 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]