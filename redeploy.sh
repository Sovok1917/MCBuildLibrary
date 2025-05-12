#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

echo "------------------------------------"
echo "🚀 Starting Redeployment Script 🚀"
echo "------------------------------------"

# --- Configuration (Optional: customize these) ---
SKIP_GIT_PULL=false
SKIP_TESTS=true
CLEAN_DOCKER_RESOURCES=false # Set to true to prune docker resources after deployment

# 1. (Optional) Pull latest changes from Git
if [ "$SKIP_GIT_PULL" = false ]; then
    echo "🔄 Pulling latest changes from Git..."
    git pull
else
    echo "⏭️ Skipping Git pull."
fi
echo "------------------------------------"

# 2. Build the Spring Boot Backend
echo "🛠️ Building Spring Boot backend..."
if [ "$SKIP_TESTS" = true ]; then
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests
    elif [ -f "./gradlew" ]; then
        ./gradlew clean build -x test
    else
        echo "⚠️ Maven or Gradle wrapper not found. Please build backend manually or update script."
        exit 1
    fi
else
    if [ -f "./mvnw" ]; then
        ./mvnw clean package
    elif [ -f "./gradlew" ]; then
        ./gradlew clean build
    else
        echo "⚠️ Maven or Gradle wrapper not found. Please build backend manually or update script."
        exit 1
    fi
fi
echo "✅ Backend build complete."
echo "------------------------------------"

# 3. Stop and Remove Existing Docker Compose Services
echo "🛑 Stopping and removing existing Docker services (if any)..."
docker-compose down --remove-orphans
echo "✅ Services stopped and removed."
echo "------------------------------------"

# 4. Start Docker Compose Services (with build)
echo "🐳 Building and starting Docker services..."
docker-compose up --build -d
echo "✅ Docker services started."
echo "------------------------------------"

# 5. (Optional) Prune unused Docker resources
if [ "$CLEAN_DOCKER_RESOURCES" = true ]; then
    echo "🧹 Pruning unused Docker images, networks, and build cache..."
    docker image prune -f
    docker network prune -f
    docker builder prune -f
    echo "✅ Docker resources pruned."
    echo "------------------------------------"
fi

echo "🎉 Redeployment complete! 🎉"
echo "Frontend should be accessible at http://localhost:3000"
echo "Backend (if directly accessed) at http://localhost:8080"
echo "------------------------------------"

# 6. (Optional) Tail logs for a few seconds or until Ctrl+C
echo "📜 Tailing backend logs (Ctrl+C to stop)..."
# Trap Ctrl+C to exit gracefully from log tailing
trap "echo 'Exiting log tail.'; exit 0" INT
docker-compose logs -f backend &
LOG_TAIL_PID=$!
wait $LOG_TAIL_PID # Wait for the log tailing process (or Ctrl+C)