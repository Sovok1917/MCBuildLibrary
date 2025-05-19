# Minecraft Build Library

The Minecraft Build Library is a full-stack web application designed for sharing and exploring Minecraft build schematics. Users can upload their builds, categorize them by authors, themes, and colors, and browse or download builds shared by others. The application features a Spring Boot backend and a React frontend.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup and Configuration](#setup-and-configuration)
  - [Backend](#backend)
  - [Frontend](#frontend)
  - [Environment Variables](#environment-variables)
- [Running the Application](#running-the-application)
  - [Using Docker Compose (Recommended)](#using-docker-compose-recommended)
  - [Running Backend and Frontend Separately](#running-backend-and-frontend-separately)
- [API Documentation](#api-documentation)
- [Key Backend Components](#key-backend-components)
- [Key Frontend Components](#key-frontend-components)
- [Scripts](#scripts)
- [Logging](#logging)
- [Contributing](#contributing)

## Features

*   **User Authentication:** Secure registration and login for users.
*   **Build Management:**
  *   Create, view, update, and delete Minecraft builds.
  *   Upload and download `.schem` files for builds.
  *   Associate builds with multiple authors, themes, and colors.
  *   Add descriptions and screenshot URLs.
*   **Metadata Management:**
  *   Manage authors, themes, and colors.
  *   Bulk creation of metadata entities.
*   **Filtering and Searching:**
  *   Filter builds by author, theme, or color.
  *   Search builds by name and metadata using fuzzy matching.
  *   Paginated results for build lists.
*   **Asynchronous Operations:**
  *   Generate detailed build log files asynchronously with task status tracking.
*   **Caching:**
  *   In-memory caching for frequently accessed data (builds, metadata, query results) to improve performance.
*   **API Logging:**
  *   Comprehensive logging of API requests and responses using AOP.
  *   Access to application logs (today's and archived by date).
*   **Visit Counting:** Simple tracking of API requests.
*   **Role-Based Access Control:** Differentiates between standard users and administrators for certain operations.
*   **SPA Frontend:** Modern React frontend with client-side routing.

## Technologies Used

*   **Backend:**
  *   Java 17+
  *   Spring Boot 3 (including Spring MVC, Spring Data JPA, Spring Security)
  *   Hibernate
  *   PostgreSQL (Database)
  *   Maven (Build Tool)
  *   Lombok
  *   AspectJ (for AOP logging)
  *   Swagger/OpenAPI (for API documentation, via `springdoc-openapi`)
*   **Frontend:**
  *   React (with Hooks, Context API)
  *   JavaScript (ES6+)
  *   Vite (Build Tool)
  *   Material UI (UI Component Library)
  *   HTML5, CSS3
*   **DevOps & Tools:**
  *   Docker & Docker Compose
  *   Git
  *   IntelliJ IDEA (IDE, as mentioned by user)
  *   Nginx (for serving frontend in Docker)

## Project Structure
```
.
├── frontend/ # React frontend application
│ ├── public/
│ ├── src/
│ │ ├── api/ # API service functions
│ │ ├── assets/
│ │ ├── components/ # React components
│ │ ├── context/ # React Context (e.g., AuthContext)
│ │ ├── hooks/ # Custom React hooks
│ │ ├── App.jsx
│ │ ├── main.jsx
│ │ └── index.css
│ ├── Dockerfile.frontend
│ ├── nginx.conf
│ ├── package.json
│ └── vite.config.js
├── src/ # Spring Boot backend application
│ ├── main/
│ │ ├── java/sovok/mcbuildlibrary/
│ │ │ ├── aspect/
│ │ │ ├── cache/
│ │ │ ├── config/
│ │ │ ├── controller/
│ │ │ ├── dto/
│ │ │ ├── exception/
│ │ │ ├── interceptor/
│ │ │ ├── model/
│ │ │ ├── repository/
│ │ │ ├── service/
│ │ │ ├── util/
│ │ │ ├── validation/
│ │ │ └── Application.java
│ │ └── resources/
│ │ ├── static/
│ │ ├── templates/ # (If using server-side templates, not primary for SPA)
│ │ ├── application.properties
│ │ └── logback-spring.xml
│ └── test/
├── .env.example # Example environment variables
├── .gitignore
├── Dockerfile.backend
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
├── redeploy.sh # Deployment script
└── README.md # This file
```
## Prerequisites

*   Java JDK 17 or newer
*   Maven 3.6+ (or use the Maven Wrapper `./mvnw`)
*   Node.js and npm (for frontend development, typically Node 20+)
*   Docker and Docker Compose (for containerized deployment)
*   PostgreSQL database instance (if not using Docker for DB)

## Setup and Configuration

### Backend

1.  The backend is a standard Spring Boot application built with Maven.
2.  Database connection details are configured in `src/main/resources/application.properties` and are expected to be supplied via environment variables (see [Environment Variables](#environment-variables)).

### Frontend

1.  The frontend is a React application managed with npm and built with Vite.
2.  Navigate to the `frontend` directory: `cd frontend`
3.  Install dependencies: `npm install`

### Environment Variables

The application uses environment variables for configuration, especially for database credentials and application defaults.

1.  Copy the `.env.example` file to a new file named `.env` in the project root:
    ```bash
    cp .env.example .env
    ```
2.  Edit the `.env` file and provide your actual configuration values:
  *   `DB_HOST`: Database host (e.g., `localhost` if running PostgreSQL locally, `host.docker.internal` if PostgreSQL is on your host machine and backend is in Docker).
  *   `DB_PORT`: Database port (default for PostgreSQL is `5432`).
  *   `DB_NAME`: Name of the database (e.g., `mcbuildlibrary`).
  *   `DB_USERNAME`: Database username.
  *   `DB_PASSWORD`: Database password.
  *   `APP_DEFAULT_ADMIN_USERNAME`: Username for the default admin user created on startup.
  *   `APP_DEFAULT_ADMIN_PASSWORD`: Password for the default admin user. **Ensure this is strong and unique.**

    The `DB_URL` will be constructed from these variables.

    **Important:** The `.env` file contains sensitive credentials and should be added to `.gitignore` to prevent committing it to version control.

## Running the Application

### Using Docker Compose (Recommended)

This is the easiest way to run the full application (backend and frontend).

1.  Ensure Docker and Docker Compose are installed and running.
2.  Make sure you have a `.env` file configured in the project root.
3.  Run the `redeploy.sh` script from the project root:
    ```bash
    bash redeploy.sh
    ```
    This script will:
  *   (Optionally) Pull latest Git changes.
  *   Build the Spring Boot backend JAR.
  *   Stop and remove any existing Docker containers for this app.
  *   Build Docker images for the backend and frontend.
  *   Start all services using `docker-compose up -d`.
  *   Tail the backend logs (press `Ctrl+C` to stop log tailing AND shut down the Docker containers).

    After the script completes:
  *   Frontend will be accessible at: `http://localhost:3000`
  *   Backend API (if accessed directly) at: `http://localhost:8080`

### Running Backend and Frontend Separately

#### Backend (Spring Boot)

1.  Ensure your PostgreSQL database is running and accessible.
2.  Set the required environment variables (e.g., `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_DEFAULT_ADMIN_USERNAME`, `APP_DEFAULT_ADMIN_PASSWORD`) in your IDE's run configuration or your shell.
3.  From the project root, run:
    ```bash
    ./mvnw spring-boot:run
    ```
    The backend will start, typically on port `8080`.

#### Frontend (React with Vite)

1.  Navigate to the `frontend` directory: `cd frontend`
2.  Start the Vite development server:
    ```bash
    npm run dev
    ```
    The frontend will start, typically on port `3000`. It is configured to proxy API requests starting with `/api` to `http://localhost:8080` (as defined in `frontend/vite.config.js`).

## API Documentation

API documentation is available via Swagger UI when the backend is running.
Access it at: `http://localhost:8080/swagger-ui.html`

## Key Backend Components

*   **Models (`sovok.mcbuildlibrary.model`)**: JPA entities representing `Build`, `User`, `Author`, `Theme`, `Color`, etc. `BaseNamedEntity` provides common fields for metadata entities.
*   **Repositories (`sovok.mcbuildlibrary.repository`)**: Spring Data JPA repositories for database interactions.
*   **Services (`sovok.mcbuildlibrary.service`)**: Business logic layer.
  *   `BuildService`: Manages build CRUD, querying, and schematic file handling.
  *   `AuthorService`, `ThemeService`, `ColorService`: Manage respective metadata entities, extending `BaseNamedEntityService` for common CRUD and caching logic.
  *   `UserService`: Handles user registration.
  *   `UserDetailsServiceImpl`: Integrates with Spring Security for user authentication.
  *   `BuildLogService`: Asynchronously generates detailed log files for builds.
  *   `VisitCounterService`: Tracks API request counts.
*   **Controllers (`sovok.mcbuildlibrary.controller`)**: Expose RESTful APIs.
  *   `BuildController`: Endpoints for build management.
  *   `AuthorController`, `ThemeController`, `ColorController`: Endpoints for metadata, extending `BaseNamedEntityController`.
  *   `UserController`: Endpoints for user registration and fetching current user details.
  *   `BuildLogController`: Endpoints for build log generation and retrieval.
  *   `LogController`: Endpoints for accessing application logs.
  *   `SpaController`: Forwards non-API requests to the frontend's `index.html` to support client-side routing.
*   **DTOs (`sovok.mcbuildlibrary.dto`)**: Data Transfer Objects for API requests and responses.
*   **Configuration (`sovok.mcbuildlibrary.config`)**:
  *   `SecurityConfig`: Configures Spring Security, CSRF protection, authentication, and authorization rules.
  *   `DataLoader`: Creates a default admin user on startup.
  *   `AsyncConfig`: Configures the thread pool for asynchronous tasks.
  *   `WebConfig`: Configures web-related beans, like interceptors.
*   **Caching (`sovok.mcbuildlibrary.cache.InMemoryCache`)**: A simple in-memory cache implementation.
*   **Aspects (`sovok.mcbuildlibrary.aspect.LoggingAspect`)**: Provides logging for service and controller method calls.
*   **Exceptions (`sovok.mcbuildlibrary.exception`)**: Custom exceptions and a `GlobalExceptionHandler` for standardized error responses.

## Key Frontend Components

*   **`App.jsx`**: Main application component, sets up routing and layout.
*   **`AuthContext.jsx`**: Manages authentication state (current user, login/logout functions).
*   **API Services (`src/api/`)**: Functions for making requests to the backend (e.g., `authService.js`, `buildService.js`).
*   **Components (`src/components/`)**:
  *   `BuildList.jsx`: Displays a list of builds.
  *   `BuildForm.jsx`: Form for creating and editing builds.
  *   `FilterSidebar.jsx`: Sidebar for filtering builds by authors, themes, and colors.
  *   `Login.jsx`, `Register.jsx`: Authentication forms.
*   **Hooks (`src/hooks/`)**:
  *   `useDebounce.js`: Custom hook for debouncing input values (e.g., for search).

## Scripts

*   **`redeploy.sh`**: A shell script to automate the build and deployment process using Docker Compose. It handles:
  *   Building the backend.
  *   Stopping and removing old Docker containers.
  *   Building and starting new Docker containers for backend and frontend.
  *   Tailing backend logs with an option to stop containers on `Ctrl+C`.

## Logging

*   **Backend Logging**: Configured via `src/main/resources/logback-spring.xml`.
  *   Logs to the console.
  *   Logs to a daily rolling file (`logs/mcbuildlibrary.log`).
  *   Archives logs daily into `logs/archive/`.
  *   Separate error log (`logs/mcbuildlibrary-error.log`).
*   **Build Log Generation**: The `BuildLogService` generates detailed text files for individual builds into the `logs/build_logs/` directory.

## Contributing

Contributions are welcome! Please follow standard Git practices (fork, branch, pull request).
Ensure code adheres to the Google Java Style Guide and SonarQube best practices for the backend.
For the frontend, follow widely accepted JavaScript/React style guides.
---