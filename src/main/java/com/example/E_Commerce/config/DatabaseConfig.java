package com.example.E_Commerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;

@Configuration
@Profile("prod")
public class DatabaseConfig {
    
    @Value("${DATABASE_URL}")
    private String databaseUrl;
    
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        try {
            System.out.println("DATABASE_URL from environment: " + databaseUrl);
            
            // Parse the Railway DATABASE_URL format: postgresql://user:pass@host:port/db
            URI dbUri = new URI(databaseUrl);
            
            String username = null;
            String password = null;
            
            // Extract credentials from URI
            if (dbUri.getUserInfo() != null) {
                String[] credentials = dbUri.getUserInfo().split(":");
                username = credentials[0];
                password = credentials.length > 1 ? credentials[1] : "";
            }
            
            // Build JDBC URL WITHOUT credentials
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d%s",
                dbUri.getHost(),
                dbUri.getPort(),
                dbUri.getPath());
            
            System.out.println("JDBC URL: " + jdbcUrl);
            System.out.println("Username: " + username);
            System.out.println("Password: " + (password != null ? "***" : "null"));
            
            // Set connection properties
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");
            
            // Connection pool settings optimized for Railway
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);
            
            // Additional settings for stability
            config.addDataSourceProperty("socketTimeout", "30");
            config.addDataSourceProperty("loginTimeout", "30");
            
            return new HikariDataSource(config);
            
        } catch (Exception e) {
            System.err.println("Failed to parse DATABASE_URL: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to configure datasource", e);
        }
    }
}
