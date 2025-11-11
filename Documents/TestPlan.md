# Test Plan for MCBuildLibrary

## 1. Introduction
This document outlines the testing strategy for the MCBuildLibrary web application. The purpose of this plan is to define the scope, approach, resources, and schedule of all testing activities. The goal is to verify that the application meets its specified requirements and quality standards before deployment.

## 2. Test Items
The items to be tested include the entire MCBuildLibrary application, which consists of the following main components:
*   **Frontend:** A React-based single-page application that provides the user interface.
*   **Backend:** A Spring Boot application that serves a REST API for all business logic.
*   **Database:** A PostgreSQL database for data persistence.

**Quality Attributes (based on ISO 25010):**
*   **Functional Suitability:** The application must perform all functions described in the SRS, such as user registration, build uploading, searching, and downloading.
*   **Security:** The application must correctly enforce access control, preventing unauthorized users from performing administrative actions.
*   **Usability:** The user interface should be intuitive and provide clear feedback for user actions.
*   **Reliability:** The application should handle errors gracefully (e.g., invalid input, server errors) without crashing.

## 3. Risk Issues
The following risks could potentially affect the quality of the product:
*   **Invalid Data:** Users might upload corrupted or non-`.schem` files, which the system must handle gracefully.
*   **Security Vulnerabilities:** A malicious user might attempt to perform actions they are not authorized for (e.g., a regular user trying to delete another user's build).
*   **Concurrency Issues:** Multiple users interacting with the system simultaneously could lead to unexpected behavior (though less critical for this project's scope).
*   **Cross-Browser Compatibility:** The frontend may not render or function correctly on all web browsers.

## 4. Features to be Tested
Testing will focus on the core functionalities of the application, derived from the Use Case analysis.
*(This list should be agreed upon with the instructor).*

*   **User Authentication:**
    *   User Registration (Positive and Negative scenarios)
    *   User Login (Positive and Negative scenarios)
    *   User Logout
*   **Build Management:**
    *   Viewing and paginating the list of builds.
    *   Searching and filtering builds.
    *   Downloading a build's schematic file.
    *   Uploading a new build (by a registered user).
*   **Administrative Functions:**
    *   Deleting any build (by an administrator).
    *   Editing any build (by an administrator).
    *   Role-based access control for admin-only functions.

## 5. Test Approach
A combination of manual and automated testing will be used.
*   **Unit Testing (Automated):** The existing JUnit tests for the backend service layer will be reviewed and potentially expanded to cover more business logic.
*   **Manual Testing:** The primary approach for this lab will be manual, black-box testing based on the test cases derived from the system's use cases. A tester will perform the role of each actor (Guest, Registered User, Administrator) and execute the scenarios defined in the `TestResults.md` document.
*   **Tools:**
    *   **Documentation:** Text editor (VS Code, etc.)
    *   **Backend Unit Testing:** JUnit, Mockito
    *   **Manual Testing:** Web Browser (e.g., Chrome, Firefox) and its developer tools.

## 6. Pass/Fail Criteria
A test case is considered **Passed** if the actual result of the test matches the expected result defined in the test case, and no critical errors occur.
A test case is considered **Failed** if the actual result deviates from the expected result. All failed tests will be documented with details to facilitate debugging.

## 7. Conclusion
This test plan provides a comprehensive framework for validating the MCBuildLibrary application. Executing these tests will ensure that the product is functional, secure, and meets the quality attributes required by the SRS.