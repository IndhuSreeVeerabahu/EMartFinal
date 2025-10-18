# Use Maven with OpenJDK 17 as base image (Eclipse Temurin)
FROM maven:3.9.4-eclipse-temurin-17

# Set working directory
WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# List files in target directory for debugging
RUN ls -la target/

# Copy the JAR file to a standard location
RUN cp target/E_Commerce-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Railway will override this with PORT environment variable)
EXPOSE 8080

# Run the application with production profile and PORT environment variable
CMD ["sh", "-c", "echo 'Starting E-Commerce application on port ${PORT:-8080}' && java -Xms256m -Xmx512m -Dspring.profiles.active=prod -Dserver.port=${PORT:-8080} -Dlogging.level.org.springframework=WARN -Djava.security.egd=file:/dev/./urandom -jar app.jar"]