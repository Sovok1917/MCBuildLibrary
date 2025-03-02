# MCBuildLibrary

*A Spring Boot application for sharing Minecraft builds through `.schematic` files.*

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/SpringBoot-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-62B47A?style=flat-square&logo=minecraft&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

## Introduction

**MCBuildLibrary** is a platform that enables Minecraft enthusiasts to share and discover builds through `.schematic` files. Whether you're showcasing your architectural masterpieces or seeking inspiration, BuildShare provides a collaborative space for the Minecraft community.

## Features

- **Upload Builds**: Share your Minecraft creations with detailed information.
- **Download Schematics**: Access and import builds into your own Minecraft worlds.
- **Detailed Build Descriptions**: Includes name, author, theme, description, main colors, `.schematic` files, and screenshots.
- **Browse and Search**: Find builds by name, author, theme, or main colors.

## Getting Started

### Prerequisites

- **Java Development Kit (JDK) 17 or higher**: [Download JDK](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
- **Maven**: [Download Maven](https://maven.apache.org/download.cgi)
- **Git**: [Download Git](https://git-scm.com/downloads)
- **IDE**: IntelliJ IDEA, Eclipse, or any Java-compatible IDE.

## Usage

### Running the Application

Start the application using Maven:

```bash
mvn spring-boot:run
```

The server will start at `http://localhost:8080`.

### API Endpoints

#### Upload a Build

- **Endpoint**: `POST /builds`
- **Description**: Upload a new build with all its details.

**Request Parameters** (as `multipart/form-data`):

- `name` (String) - *Required*
- `author` (String) - *Required*
- `theme` (String)
- `description` (String)
- `mainColors` (List of Strings)
- `schematicFile` (File) - *Required* (`.schematic` file)
- `screenshots` (List of Files) - Images of the build

## Acknowledgments

- **Minecraft Community**: For the endless creativity and inspiration.
- **Spring Boot Documentation**: For comprehensive guides and reference.
- **Open-Source Libraries**: For the tools that make development easier.

---
