Let’s update the `README.md` file for your `MCBuildLibrary` project to reflect its current state, improve clarity, and add some enhancements to make it more professional and user-friendly. I’ll ensure the content aligns with the project’s features (e.g., updated field names like `themes` instead of `theme`, `authors` instead of `author`, etc.) and include additional sections for better documentation.

Here’s the revised `README.md`:

---

# MCBuildLibrary

*A Spring Boot application for sharing Minecraft builds through `.schematic` files.*

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/SpringBoot-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-62B47A?style=flat-square&logo=minecraft&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=flat-square&logo=mariadb&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

## Introduction

**MCBuildLibrary** is a platform designed for Minecraft enthusiasts to share, discover, and manage builds using `.schematic` files. Whether you're showcasing your architectural masterpieces or seeking inspiration for your next project, MCBuildLibrary provides a collaborative space for the Minecraft community to connect and create.

## Features

- **Upload Builds**: Share your Minecraft creations with detailed metadata, including `.schematic` files and screenshots.
- **Download Schematics**: Access and import builds into your own Minecraft worlds using `.schematic` files.
- **Detailed Build Information**: Each build includes:
  - Name
  - Authors (supports multiple authors)
  - Themes (e.g., Medieval, Modern)
  - Description
  - Main Colors
  - Screenshots (images of the build)
  - `.schematic` file for the build
- **Browse and Search**: Find builds by name, author, theme, or main colors (search functionality to be implemented in future updates).
- **RESTful API**: Interact with the platform programmatically using well-defined API endpoints.
- **Database Integration**: Uses MariaDB to persistently store build data and relationships.
- **Lazy Loading**: Optimized data retrieval with JPA lazy loading for related entities (e.g., authors, themes, colors).
- **Cascading Operations**: Simplified management of related entities with JPA cascading for persistence and updates.

## Getting Started

### Prerequisites

- **Java Development Kit (JDK) 23 or higher**: [Download JDK](https://www.oracle.com/java/technologies/javase/jdk23-downloads.html)  
  *(The project currently uses JDK 23, as seen in the logs.)*
- **Maven**: [Download Maven](https://maven.apache.org/download.cgi)
- **Git**: [Download Git](https://git-scm.com/downloads)
- **MariaDB**: [Download MariaDB](https://mariadb.org/download/)  
  *(The project uses MariaDB as the database, as seen in the logs.)*
- **IDE**: IntelliJ IDEA, Eclipse, or any Java-compatible IDE.

### Installation

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Sovok1917/MCBuildLibrary.git
   cd MCBuildLibrary
   ```
   2. **Set Up the Database**:
   - Install MariaDB and start the server.
   - Create a database named `mcbuildlibrary`:
     ```sql
     CREATE DATABASE mcbuildlibrary;
     ```
   - Update the database configuration in `src/main/resources/application.properties`:
     ```properties
     spring.datasource.url=jdbc:mariadb://localhost:3306/mcbuildlibrary
     spring.datasource.username=your_username
     spring.datasource.password=your_password
     spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
     spring.jpa.hibernate.ddl-auto=create-drop
     spring.jpa.show-sql=true
     spring.jpa.properties.hibernate.format_sql=true
     logging.level.org.hibernate.SQL=DEBUG
     logging.level.org.hibernate.orm.jdbc.bind=TRACE
     ```
     *(Replace `your_username` and `your_password` with your MariaDB credentials.)*

3. **Install Dependencies**:
   Run the following command to download all required dependencies using Maven:
   ```bash
   mvn install
   ```

## Usage

### Running the Application

Start the application using Maven:

```bash
mvn spring-boot:run
```

The server will start at `http://localhost:8080`.

### API Endpoints

The application provides a RESTful API to manage Minecraft builds. All endpoints are accessible under the `/builds` base path.

#### **Create a Build**
- **Endpoint**: `POST /builds`
- **Content-Type**: `multipart/form-data`
- **Request Parameters**:
  - `name` (String, required): The name of the build (e.g., "Castle").
  - `authors` (List of Strings, required): The authors of the build (e.g., `["Author1", "Author2"]`).
  - `themes` (List of Strings, required): The themes of the build (e.g., `["Medieval"]`).
  - `description` (String, optional): A description of the build (e.g., "A castle").
  - `colors` (List of Strings, required): The main colors of the build (e.g., `["Gray", "Brown"]`).
  - `screenshots` (List of Strings, optional): URLs or identifiers for screenshots of the build (e.g., `["shot1", "shot2"]`).
  - `schemFile` (File, required): The `.schematic` file for the build.
- **Example Request** (using `curl`):
  ```bash
  curl -X POST http://localhost:8080/builds \
    -F "name=Castle" \
    -F "authors=Author1" \
    -F "authors=Author2" \
    -F "themes=Medieval" \
    -F "description=A castle" \
    -F "colors=Gray" \
    -F "colors=Brown" \
    -F "screenshots=shot1" \
    -F "screenshots=shot2" \
    -F "schemFile=@/path/to/castle.schematic"
  ```
- **Response**:
  - Status: `201 Created`
  - Body: The created `Build` object in JSON format.

#### **Get All Builds**
- **Endpoint**: `GET /builds`
- **Response**:
  - Status: `200 OK`
  - Body: A list of all `Build` objects in JSON format.

#### **Get a Build by ID**
- **Endpoint**: `GET /builds/{id}`
- **Path Parameter**:
  - `id` (Long): The ID of the build to retrieve.
- **Response**:
  - Status: `200 OK`
  - Body: The `Build` object in JSON format.
  - Status: `404 Not Found` if the build does not exist.

#### **Update a Build**
- **Endpoint**: `PUT /builds/{id}`
- **Content-Type**: `multipart/form-data`
- **Path Parameter**:
  - `id` (Long): The ID of the build to update.
- **Request Parameters**: Same as the `POST /builds` endpoint.
- **Response**:
  - Status: `200 OK`
  - Body: The updated `Build` object in JSON format.
  - Status: `404 Not Found` if the build does not exist.

#### **Delete a Build**
- **Endpoint**: `DELETE /builds/{id}`
- **Path Parameter**:
  - `id` (Long): The ID of the build to delete.
- **Response**:
  - Status: `204 No Content`
  - Status: `404 Not Found` if the build does not exist.

### Example Build JSON
```json
{
  "id": 1,
  "name": "Castle",
  "authors": [
    {"id": 1, "name": "Author1"},
    {"id": 2, "name": "Author2"}
  ],
  "themes": [
    {"id": 1, "name": "Medieval"}
  ],
  "description": "A castle",
  "colors": [
    {"id": 1, "name": "Gray"},
    {"id": 2, "name": "Brown"}
  ],
  "screenshots": ["shot1", "shot2"]
}
```

## Project Structure

```
MCBuildLibrary/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── sovok/mcbuildlibrary/
│   │   │       ├── Application.java         # Main application class
│   │   │       ├── controller/              # REST controllers
│   │   │       ├── entity/                  # JPA entities (Build, Author, Theme, Color)
│   │   │       ├── repository/              # JPA repositories
│   │   │       ├── service/                 # Business logic services
│   │   │       └── exception/               # Custom exceptions
│   │   └── resources/
│   │       ├── application.properties       # Configuration file
│   │       └── static/                      # Static resources (e.g., index.html)
│   └── test/                                # Unit and integration tests
├── pom.xml                                  # Maven configuration
└── README.md                                # Project documentation
```

## Database Schema

The application uses MariaDB to store data. The schema includes the following tables:
- `build`: Stores build metadata (id, name, description, schem_file).
- `author`: Stores author information (id, name).
- `theme`: Stores theme information (id, name).
- `color`: Stores color information (id, name).
- `build_authors`: Join table for the many-to-many relationship between `build` and `author`.
- `build_themes`: Join table for the many-to-many relationship between `build` and `theme`.
- `build_colors`: Join table for the many-to-many relationship between `build` and `color`.
- `build_screenshots`: Stores screenshots for each build (as an `@ElementCollection`).

## Future Improvements

- **Search Functionality**: Add endpoints to search builds by name, author, theme, or color.
- **Pagination**: Implement pagination for the `GET /builds` endpoint to handle large datasets.
- **File Storage**: Store `.schematic` files and screenshots on a file system or cloud storage (e.g., AWS S3) instead of in the database.
- **User Authentication**: Add user authentication and authorization to manage access to builds.
- **Frontend Interface**: Develop a web-based frontend to interact with the API (e.g., using React or Angular).
- **Performance Optimization**: Use fetch joins or DTOs to optimize data retrieval and serialization.

## Contributing

Contributions are welcome! To contribute:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Make your changes and commit them (`git commit -m "Add your feature"`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a pull request.

Please ensure your code follows the project’s coding standards and includes appropriate tests.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **Minecraft Community**: For the endless creativity and inspiration.
- **Spring Boot Documentation**: For comprehensive guides and reference.
- **Open-Source Libraries**: For the tools that make development easier, including Spring Boot, Hibernate, and MariaDB.
