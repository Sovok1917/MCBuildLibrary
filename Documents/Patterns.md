# Applied Patterns and Templates

This document identifies the key design patterns, architectural patterns, and refactoring templates used in the MCBuildLibrary project

## 1. Architectural Patterns

### Layered Architecture
The application is structured in a classic layered architecture, which is a fundamental pattern for creating maintainable enterprise applications. This separates concerns and makes the system easier to manage.
*   **Presentation Layer:** `*Controller` classes (e.g., `BuildController`) handle incoming HTTP requests.
*   **Service (Business Logic) Layer:** `*Service` classes (e.g., `BuildService`) contain the core application logic.
*   **Data Access Layer:** `*Repository` interfaces (e.g., `BuildRepository`) handle communication with the database.

### Model-View-Controller (MVC)
The Spring Boot backend is built upon the MVC pattern.
*   **Model:** The domain classes like `Build`, `User`, `Author`, etc., represent the data structure.
*   **View:** In our RESTful application, the "view" is the JSON data that is serialized and sent to the frontend client.
*   **Controller:** The `*Controller` classes process user input (HTTP requests) and orchestrate the response by interacting with the service layer.

## 2. Design Patterns (GoF and others)

### Dependency Injection (DI)
This is a core pattern used throughout the Spring Boot application. Instead of classes creating their own dependencies, the Spring IoC (Inversion of Control) container "injects" them. This promotes loose coupling and makes testing easier.
*   **Example:** The `BuildService` is injected into the `BuildController` via its constructor.

### Singleton
By default, Spring beans (like our services, controllers, and repositories) are managed as Singletons. This means only one instance of each class is created and shared throughout the application, which is efficient for stateless services.

### Repository
The Data Access Layer is implemented using the Repository pattern via Spring Data JPA. Interfaces like `BuildRepository` abstract the data source and provide a clean API for CRUD (Create, Read, Update, Delete) operations without needing to write boilerplate SQL.

### Data Transfer Object (DTO)
The project uses DTOs to transfer data between the client and the server. This is a crucial pattern for decoupling the database entities from the API contract.
*   **Example:** `UserRegistrationDto` is used to safely transport registration data from the frontend to the `UserController`.

### Builder
The Lombok `@Builder` annotation on model classes like `User` and `Build` implements the Builder pattern. This allows for the clean, readable construction of complex objects.
*   **Example:** `User.builder().username("admin").password("...").build();`

### Observer
The React frontend implicitly uses the Observer pattern. The `useEffect` hook "observes" state variables (like `activeFilter` or `currentPage`). When these variables change, the component automatically re-renders or re-fetches data, keeping the UI in sync with the application state.

## 3. Refactoring Templates

### Extract Method
This refactoring pattern is used to break down long, complex methods into smaller, more manageable ones with clear names.
*   **Example:** In the `BuildLogService`, the complex logic of generating a log file is broken down. The main `performLogGeneration` method calls a helper method `buildLogContent`, which in turn calls even smaller helpers like `appendCollectionDetails` and `appendDescription`. This makes the code much cleaner and easier to read.