package com.example.demo;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;

import org.springframework.stereotype.Component;

@Component
public class DbEnvCheck {
    @PostConstruct
    public void init() {
        System.out.println("===== Environment Variables =====");
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USERNAME");
        String dbPass = System.getenv("DB_PASSWORD");
        System.out.println("DB_URL: " + dbUrl);
        System.out.println("DB_USERNAME: " + dbUser);
        System.out.println("DB_PASSWORD: " + (dbPass != null ? "*****" : null));
        System.out.println("=================================");

        // Optional: DB 연결 테스트
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            System.out.println("✅ DB 연결 성공!");
        } catch (Exception e) {
            System.out.println("❌ DB 연결 실패: " + e.getMessage());
        }
    }
}
