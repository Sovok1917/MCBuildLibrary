# Flow of Events

This document provides detailed scenarios for the key use cases identified in the Use Case Diagram. Each scenario describes the step-by-step interaction between an actor and the system.

## Table of Contents
1. [Upload New Build](#upload-new-build)
2. [Delete Build](#delete-build)
3. [Search & Filter Builds](#search-filter-builds)
4. [Login](#login)

---

<a name="upload-new-build"/>

### 1. Use Case: Upload New Build

**Actor:** Registered User

**Preconditions:**
*   The user is successfully logged into the system.

**Postconditions (on success):**
*   A new Build entity, with all its metadata and schematic file, is saved to the database.
*   The user is shown a success message.

**Main Flow:**
1.  The user clicks the "Add New Build" button on the main page.
2.  The system displays the build creation form, containing fields for "Build Name", "Authors", "Themes", "Colors", "Description", "Screenshot URLs", and an "Upload Schematic File" button.
3.  The user fills in all required fields (Name, Authors, Themes, Colors).
4.  The user clicks the "Upload Schematic File" button and selects a `.schem` file from their local machine.
5.  The user clicks the "Create Build" button.
6.  The system validates that the submitted data is correct (e.g., the name is not empty).
7.  The system verifies that a build with the same name does not already exist in the database.
8.  The system saves the schematic file and all provided information as a new Build record in the database.
9.  The system hides the form and displays a success message on the page, e.g., "Build 'MyAwesomeCastle' created successfully!".
10. The system refreshes the build list to show the newly added build.

**Alternative Flows:**
*   **A1: User cancels creation**
    1.  At step 5 of the Main Flow, the user clicks the "Cancel" button.
    2.  The system hides the creation form and returns the user to the previous page without saving any data.

**Exception Flows:**
*   **E1: Invalid data submitted**
    1.  At step 6 of the Main Flow, the system detects that a required field is empty.
    2.  The system aborts the save process and displays an error message next to the invalid field (e.g., "Build Name cannot be blank").

*   **E2: Build name already exists**
    1.  At step 7 of the Main Flow, the system finds an existing build with the same name.
    2.  The system aborts the save process and displays an error message above the form: "A build with this name already exists. Please choose a unique name."

---

<a name="delete-build"/>

### 2. Use Case: Delete Build

**Actor:** Administrator

**Preconditions:**
*   The Administrator is successfully logged into the system.
*   The build to be deleted exists in the system.

**Postconditions (on success):**
*   The specified build is permanently removed from the database.
*   The user is shown a success message.

**Main Flow:**
1.  The Administrator locates the build they wish to delete in the build list.
2.  The Administrator clicks the "Delete" icon/button for that specific build.
3.  The system displays a confirmation dialog: "Are you sure you want to delete the build '[Build Name]'? This action cannot be undone."
4.  The Administrator confirms the action by clicking "Confirm" or "Yes".
5.  The system verifies that the current user has `ROLE_ADMIN` permissions.
6.  The system sends a request to the server to delete the build from the database.
7.  The server removes the build record and its associated schematic file.
8.  The system displays a success message: "Build was successfully deleted."
9.  The system refreshes the build list, and the deleted build is no longer visible.

**Alternative Flows:**
*   **A1: Administrator cancels deletion**
    1.  At step 4 of the Main Flow, the Administrator clicks "Cancel" in the confirmation dialog.
    2.  The system closes the dialog, and no changes are made.

**Exception Flows:**
*   **E1: User lacks permission**
    1.  (Hypothetical) If a non-admin user could somehow trigger this action, at step 5, the system would check their permissions and find they are not an Administrator.
    2.  The system aborts the action and displays an error message: "You do not have permission to perform this action."

*   **E2: Build was already deleted**
    1.  At step 6, the server cannot find the build to delete (e.g., it was deleted by another administrator moments before).
    2.  The server returns a "Not Found" error. The system displays a message: "Error: This build no longer exists."