# Minecraft Build Library

The Minecraft Build Library is a full-stack web application designed for sharing and exploring Minecraft build schematics. Users can upload their builds, categorize them by authors, themes, and colors, and browse or download builds shared by others. The application features a Spring Boot backend and a React frontend.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup and Configuration](#setup-and-configuration)
    - [Environment Variables](#environment-variables)
- [Running the Application](#running-the-application)
    - [Using Docker Compose (Recommended)](#using-docker-compose-recommended)
    - [Running Backend and Frontend Separately](#running-backend-and-frontend-separately)
- [API Documentation](#api-documentation)
- [Key Backend Components](#key-backend-components)
- [Key Frontend Components](#key-frontend-components)
- [Scripts](#scripts)
- [Logging](#logging)
- [License](#license)

## Features

*   **User Authentication:** Secure registration and login for users using Spring Security.
*   **Build Management:**
    *   Create, view, update, and delete Minecraft builds.
    *   Upload and download `.schem` files for builds.
    *   Associate builds with multiple authors, themes, and colors.
    *   Add descriptions and comma-separated screenshot URLs.
*   **Metadata Management:**
    *   Administrators can manage authors, themes, and colors directly from the UI sidebar.
    *   Bulk creation of metadata entities via a dedicated API endpoint.
*   **Filtering and Searching:**
    *   Filter builds by clicking on an author, theme, or color in the sidebar.
    *   Search builds by name or using keyword filters (e.g., `My Castle author:BuilderBob theme:Medieval`).
    *   Paginated results for all build lists to ensure performance.
*   **Asynchronous Operations:**
    *   Generate detailed build log files asynchronously with task status tracking.
*   **Caching:**
    *   In-memory caching for frequently accessed data (builds, metadata, query results) to improve performance.
*   **API Logging:**
    *   Comprehensive logging of API requests and responses using AOP.
    *   API endpoints to access application logs (today's and archived by date).
*   **Visit Counting:** Simple tracking of total API requests handled since application startup.
*   **Role-Based Access Control:** Differentiates between standard users (`ROLE_USER`) and administrators (`ROLE_ADMIN`) for operations like editing/deleting builds and managing metadata.
*   **SPA Frontend:** Modern React frontend with client-side routing, built with Vite and Material UI.

## Technologies Used

*   **Backend:**
    *   Java 17
    *   Spring Boot 3.4.3 (including Spring MVC, Spring Data JPA, Spring Security)
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
    *   Nginx (for serving the production frontend build in Docker)

## Project Structure
```
├── frontend/              # React frontend application
│   ├── src/
│   │   ├── api/           # API service functions
│   │   ├── components/    # React components
│   │   ├── context/       # AuthContext for state management
│   │   ├── hooks/         # Custom React hooks (useDebounce)
│   │   └── ...
│   ├── Dockerfile.frontend
│   ├── nginx.conf         # Nginx config for serving the React app
│   ├── package.json
│   └── vite.config.js
├── src/                   # Spring Boot backend application
│   ├── main/
│   │   ├── java/sovok/mcbuildlibrary/
│   │   │   ├── aspect/
│   │   │   ├── cache/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── Application.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback-spring.xml
│   └── test/
├── .env.example           # Example environment variables
├── .gitignore
├── Dockerfile.backend
├── docker-compose.yml
├── mvnw                   # Maven wrapper scripts
├── pom.xml
├── redeploy.sh            # Deployment script
└── README.md              # This file
```

## Prerequisites

*   Java JDK 17 or newer
*   Maven 3.6+ (or use the included Maven Wrapper `./mvnw`)
*   Node.js and npm (for frontend development, Node 20+ recommended)
*   Docker and Docker Compose
*   A running PostgreSQL database instance (if not using Docker for the database)

## Setup and Configuration

### Environment Variables

The application relies on environment variables for configuration.

1.  Copy the `.env.example` file to a new file named `.env` in the project root:
    ```bash
    cp .env.example .env
    ```
2.  Edit the `.env` file and provide your actual configuration values:
    *   `DB_HOST`: Database host (use `host.docker.internal` if the database is on your host machine and the backend is in Docker).
    *   `DB_PORT`: Database port (default for PostgreSQL is `5432`).
    *   `DB_NAME`: Name of the database.
    *   `DB_USERNAME`: Database username.
    *   `DB_PASSWORD`: Database password.
    *   `APP_DEFAULT_ADMIN_USERNAME`: Username for the default admin user created on startup.
    *   `APP_DEFAULT_ADMIN_PASSWORD`: Password for the default admin user. **Ensure this is strong and unique.**

    **Note:** If you are not using Docker for your database, you will need to manually create the database specified in `DB_NAME` before starting the backend.

## Running the Application

### Using Docker Compose (Recommended)

This is the simplest method to run the entire application.

1.  Ensure Docker and Docker Compose are installed and running.
2.  Confirm your `.env` file is correctly configured in the project root.
3.  Execute the `redeploy.sh` script from the project root:
    ```bash
    bash redeploy.sh
    ```
    This script automates the entire process:
    *   Builds the Spring Boot backend JAR file.
    *   Stops and removes any existing Docker containers for this application.
    *   Builds fresh Docker images for both the backend and frontend.
    *   Starts all services in detached mode.
    *   Tails the backend logs for you. Press `Ctrl+C` to stop tailing the logs and gracefully shut down all Docker containers.

    Once running:
    *   **Frontend:** `http://localhost:3000`
    *   **Backend API:** `http://localhost:8080`

### Running Backend and Frontend Separately

#### Backend (Spring Boot)

1.  Ensure your PostgreSQL database is running and accessible.
2.  Set the required environment variables (e.g., `DB_URL`, `DB_USERNAME`, etc.) in your IDE's run configuration or your shell.
3.  From the project root, run:
    ```bash
    ./mvnw spring-boot:run
    ```
    The backend will start on port `8080`.

#### Frontend (React with Vite)

1.  Navigate to the `frontend` directory: `cd frontend`
2.  Install dependencies: `npm install`
3.  Start the Vite development server:
    ```bash
    npm run dev
    ```
    The frontend will start on port `3000`. It is configured in `vite.config.js` to proxy API requests from `/api` to the backend at `http://localhost:8080`.

## API Documentation

Comprehensive API documentation is generated via Swagger UI. With the backend running, access it at:
**`http://localhost:8080/swagger-ui.html`**

## Key Backend Components

*   **Models (`model`)**: JPA entities like `Build`, `User`, and `BaseNamedEntity` which provides common fields (`id`, `name`) for metadata entities like `Author`, `Theme`, and `Color`.
*   **Repositories (`repository`)**: Spring Data JPA interfaces for database operations.
*   **Services (`service`)**: Contains the core business logic.
    *   `BaseNamedEntityService`: An abstract class providing reusable CRUD, caching, and bulk creation logic for metadata entities.
    *   `BuildService`: Manages all operations related to builds.
    *   `UserService` & `UserDetailsServiceImpl`: Handle user registration and Spring Security integration.
    *   `BuildLogService`: Manages asynchronous generation of build log files.
*   **Controllers (`controller`)**: Expose the RESTful API endpoints.
    *   `BaseNamedEntityController`: An abstract class providing common REST endpoints for metadata entities.
    *   `SpaController`: A crucial controller that forwards non-API requests to the frontend's `index.html`, enabling client-side routing in the React app.
*   **Configuration (`config`)**:
    *   `SecurityConfig`: Defines all security rules, including endpoint permissions, CSRF protection, and login/logout behavior.
    *   `DataLoader`: Creates a default admin user on application startup using credentials from the environment variables.
*   **Caching (`cache.InMemoryCache`)**: A simple, thread-safe in-memory cache to reduce database queries for frequently accessed data.
*   **Aspects (`aspect.LoggingAspect`)**: Uses AOP to log method entry, exit, and exceptions for services and controllers, keeping business logic clean.

## Key Frontend Components

*   **`App.jsx`**: The main application component that sets up routing and the main layout, including the AppBar and search bar.
*   **`AuthContext.jsx`**: A React Context that provides global authentication state (current user, roles, loading status) and functions (`login`, `logout`) to the entire application.
*   **API Services (`src/api/`)**: A set of files (`authService.js`, `buildService.js`, etc.) that contain functions for making structured API calls to the backend.
*   **Components (`src/components/`)**:
    *   `BuildList.jsx` & `BuildForm.jsx`: Handle the display, creation, and editing of builds.
    *   `FilterSidebar.jsx`: A dynamic sidebar that fetches and displays filterable metadata, including administrative controls for editing and deleting items.
    *   `Login.jsx` & `Register.jsx`: User authentication forms.
*   **Hooks (`src/hooks/useDebounce.js`)**: A custom hook to delay execution of the search function, preventing excessive API calls while the user is typing.

## Scripts

*   **`redeploy.sh`**: A powerful shell script that automates the entire build and deployment lifecycle using Docker Compose. It is the recommended way to run the application for a production-like setup.

## Logging

*   **Backend Application Logs**: Configured via `logback-spring.xml`. Logs are output to the console and to daily rolling files in the `logs/` directory, with archives stored in `logs/archive/`.
*   **Build-Specific Logs**: The `BuildLogService` can generate detailed text files for individual builds, which are stored in the `logs/build_logs/` directory.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.