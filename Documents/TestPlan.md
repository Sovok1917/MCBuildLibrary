# Test Results for MCBuildLibrary

This document contains the detailed test cases executed for the MCBuildLibrary project and their outcomes.

| ID | Purpose / Title | Scenario / Instructions | Expected Result | Actual Result | Pass/Fail |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Authentication** |
| TC-01 | Successful Registration | 1. Navigate to the Register page. <br> 2. Enter a unique username and a matching password/confirm password. <br> 3. Click "Register". | A success message is displayed, and the user is redirected to the Login page. | | |
| TC-02 | Registration with existing username | 1. Navigate to the Register page. <br> 2. Enter a username that already exists. <br> 3. Click "Register". | An error message "Username already taken" is displayed. The user remains on the Register page. | | |
| TC-03 | Successful Login | 1. Navigate to the Login page. <br> 2. Enter the credentials of a valid user. <br> 3. Click "Login". | The user is redirected to the main page and sees a welcome message. The "Logout" button is visible. | | |
| TC-04 | Login with invalid credentials | 1. Navigate to the Login page. <br> 2. Enter an incorrect username or password. <br> 3. Click "Login". | An error message "Invalid credentials" is displayed. The user remains on the Login page. | | |
| **Build Management** |
| TC-05 | View Builds as Guest | 1. Open the application without logging in. | A paginated list of builds is displayed on the main page. | | |
| TC-06 | Download Schematic as Guest | 1. As a Guest, click the "Download" icon on any build. | The browser prompts to save the correct `.schem` file. | | |
| TC-07 | Successful Build Upload | 1. Log in as a Registered User. <br> 2. Click "Add New Build". <br> 3. Fill all required fields with valid data and select a valid `.schem` file. <br> 4. Click "Create Build". | A success message is shown, and the new build appears in the build list. | | |
| TC-08 | Build Upload with missing data | 1. Log in as a Registered User. <br> 2. Attempt to submit the "Add New Build" form with a required field (e.g., Name) left blank. | A validation error message is displayed next to the empty field. The form is not submitted. | | |
| **Security & Admin** |
| TC-09 | Attempt Admin action as User | 1. Log in as a regular (non-admin) Registered User. <br> 2. Attempt to access an admin function (e.g., by trying to manually send a DELETE request to `/api/builds/{id}`). | The server should return a `403 Forbidden` error. The UI should not show admin buttons (like Delete/Edit on other's builds). | | |
| TC-10 | Successful Build Deletion by Admin | 1. Log in as an Administrator. <br> 2. Click the "Delete" icon on a build. <br> 3. Confirm the action in the dialog. | The build is removed from the list. | | |