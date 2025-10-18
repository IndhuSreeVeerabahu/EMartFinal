package com.example.E_Commerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Handle Railway DATABASE_URL format - it already contains credentials
        String jdbcUrl;
        
        // Debug logging
        System.out.println("DATABASE_URL from environment: " + (databaseUrl != null ? databaseUrl : "null"));
        
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            if (databaseUrl.startsWith("postgresql://")) {
                jdbcUrl = "jdbc:" + databaseUrl;
            } else if (databaseUrl.startsWith("jdbc:postgresql://")) {
                // Check if URL has credentials, if not add them
                if (!databaseUrl.contains("@")) {
                    // URL is missing credentials, add them
                    jdbcUrl = "jdbc:postgresql://postgres:dzVSSNhjjQDshMpaVvMZapwCXnqlQrJR@shortline.proxy.rlwy.net:35449/railway";
                } else {
                    jdbcUrl = databaseUrl;
                }
            } else {
                // Fallback to new database values
                jdbcUrl = "jdbc:postgresql://postgres:dzVSSNhjjQDshMpaVvMZapwCXnqlQrJR@shortline.proxy.rlwy.net:35449/railway";
            }
        } else {
            // Fallback to new database values
            jdbcUrl = "jdbc:postgresql://postgres:dzVSSNhjjQDshMpaVvMZapwCXnqlQrJR@shortline.proxy.rlwy.net:35449/railway";
        }
        
        System.out.println("Final JDBC URL: " + jdbcUrl);
        
        config.setJdbcUrl(jdbcUrl);
        // No need to set username/password separately since they're in the URL
        config.setDriverClassName("org.postgresql.Driver");
        
        // Connection pool settings
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        config.setInitializationFailTimeout(1);
        
        return new HikariDataSource(config);
    }
}
