#!/bin/bash

# Production Deployment Script for E-Commerce Application
# Usage: ./deploy.sh [environment] [action]
# Example: ./deploy.sh prod start

set -e

# Configuration
APP_NAME="ecommerce"
APP_VERSION="1.0.0"
DOCKER_COMPOSE_FILE="docker-compose.yml"
ENVIRONMENT=${1:-prod}
ACTION=${2:-start}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if required tools are installed
check_requirements() {
    log "Checking requirements..."
    
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    if [ ! -f ".env" ]; then
        warning ".env file not found. Creating from env.example..."
        if [ -f "env.example" ]; then
            cp env.example .env
            warning "Please update .env file with your actual values before proceeding."
            exit 1
        else
            error "env.example file not found. Please create .env file manually."
            exit 1
        fi
    fi
    
    success "Requirements check passed."
}

# Build the application
build_app() {
    log "Building application..."
    
    # Build with Maven
    if [ -f "pom.xml" ]; then
        log "Building with Maven..."
        ./mvnw clean package -DskipTests -Pprod
        success "Maven build completed."
    else
        error "pom.xml not found. Please run this script from the project root directory."
        exit 1
    fi
    
    # Build Docker image
    log "Building Docker image..."
    docker build -t ${APP_NAME}:${APP_VERSION} .
    docker tag ${APP_NAME}:${APP_VERSION} ${APP_NAME}:latest
    success "Docker image built successfully."
}

# Start the application
start_app() {
    log "Starting application..."
    
    # Pull latest images
    docker-compose -f ${DOCKER_COMPOSE_FILE} pull
    
    # Start services
    docker-compose -f ${DOCKER_COMPOSE_FILE} up -d
    
    # Wait for services to be healthy
    log "Waiting for services to be healthy..."
    sleep 30
    
    # Check if application is running
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        success "Application started successfully!"
        log "Application is available at: http://localhost:8080"
        log "Admin panel: http://localhost:8080/admin/dashboard"
        log "API documentation: http://localhost:8080/swagger-ui.html"
    else
        error "Application failed to start. Check logs with: docker-compose logs"
        exit 1
    fi
}

# Stop the application
stop_app() {
    log "Stopping application..."
    docker-compose -f ${DOCKER_COMPOSE_FILE} down
    success "Application stopped successfully."
}

# Restart the application
restart_app() {
    log "Restarting application..."
    stop_app
    sleep 5
    start_app
}

# Update the application
update_app() {
    log "Updating application..."
    
    # Pull latest code (if using git)
    if [ -d ".git" ]; then
        log "Pulling latest code..."
        git pull origin main
    fi
    
    # Rebuild and restart
    build_app
    restart_app
}

# Show application status
status_app() {
    log "Application Status:"
    docker-compose -f ${DOCKER_COMPOSE_FILE} ps
    
    echo ""
    log "Application Health:"
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        success "Application is healthy"
        curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || curl -s http://localhost:8080/actuator/health
    else
        error "Application is not responding"
    fi
}

# Show application logs
logs_app() {
    log "Showing application logs..."
    docker-compose -f ${DOCKER_COMPOSE_FILE} logs -f
}

# Backup application data
backup_app() {
    log "Creating backup..."
    
    BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p ${BACKUP_DIR}
    
    # Backup database
    log "Backing up database..."
    docker-compose -f ${DOCKER_COMPOSE_FILE} exec mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} ECommerceDB > ${BACKUP_DIR}/database.sql
    
    # Backup application logs
    log "Backing up logs..."
    docker-compose -f ${DOCKER_COMPOSE_FILE} logs > ${BACKUP_DIR}/application.log
    
    # Create backup archive
    tar -czf ${BACKUP_DIR}.tar.gz ${BACKUP_DIR}
    rm -rf ${BACKUP_DIR}
    
    success "Backup created: ${BACKUP_DIR}.tar.gz"
}

# Restore application data
restore_app() {
    BACKUP_FILE=$1
    
    if [ -z "$BACKUP_FILE" ]; then
        error "Please specify backup file: ./deploy.sh restore backup_file.tar.gz"
        exit 1
    fi
    
    if [ ! -f "$BACKUP_FILE" ]; then
        error "Backup file not found: $BACKUP_FILE"
        exit 1
    fi
    
    log "Restoring from backup: $BACKUP_FILE"
    
    # Extract backup
    BACKUP_DIR=$(basename "$BACKUP_FILE" .tar.gz)
    tar -xzf "$BACKUP_FILE"
    
    # Restore database
    log "Restoring database..."
    docker-compose -f ${DOCKER_COMPOSE_FILE} exec -T mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} ECommerceDB < ${BACKUP_DIR}/database.sql
    
    # Cleanup
    rm -rf ${BACKUP_DIR}
    
    success "Restore completed successfully."
}

# Clean up old resources
cleanup() {
    log "Cleaning up old resources..."
    
    # Remove unused Docker images
    docker image prune -f
    
    # Remove unused Docker volumes
    docker volume prune -f
    
    # Remove old backups (keep last 7 days)
    find backups/ -name "*.tar.gz" -mtime +7 -delete 2>/dev/null || true
    
    success "Cleanup completed."
}

# Show help
show_help() {
    echo "E-Commerce Application Deployment Script"
    echo ""
    echo "Usage: $0 [environment] [action]"
    echo ""
    echo "Environments:"
    echo "  prod    Production environment (default)"
    echo "  staging Staging environment"
    echo "  dev     Development environment"
    echo ""
    echo "Actions:"
    echo "  start     Start the application"
    echo "  stop      Stop the application"
    echo "  restart   Restart the application"
    echo "  update    Update and restart the application"
    echo "  build     Build the application"
    echo "  status    Show application status"
    echo "  logs      Show application logs"
    echo "  backup    Create application backup"
    echo "  restore   Restore from backup"
    echo "  cleanup   Clean up old resources"
    echo "  help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 prod start"
    echo "  $0 prod update"
    echo "  $0 prod backup"
    echo "  $0 prod restore backup_20240101_120000.tar.gz"
}

# Main script logic
main() {
    case $ACTION in
        start)
            check_requirements
            start_app
            ;;
        stop)
            stop_app
            ;;
        restart)
            restart_app
            ;;
        update)
            check_requirements
            update_app
            ;;
        build)
            check_requirements
            build_app
            ;;
        status)
            status_app
            ;;
        logs)
            logs_app
            ;;
        backup)
            backup_app
            ;;
        restore)
            restore_app $3
            ;;
        cleanup)
            cleanup
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            error "Unknown action: $ACTION"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main
