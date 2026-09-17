# NexusForge Architecture Audit

## 1. Existing Architecture Overview
The current NexusForge implementation consists of a React + Vite frontend and a Spring Boot 3 + Java 21 backend.
The core architecture follows an AI-assisted pattern where the frontend sends natural language to the backend, which parses it using an LLM into an `AdapterSpecification`. This specification is then used by the `AdapterGeneratorService` to resolve generic templates (Freemarker) and output a Maven workspace, which is then built. 

## 2. Existing Six-Step Implementation
The frontend fully implements the six-step wizard exactly as required:
1. `RequirementForm.jsx` (Natural Language Requirement)
2. `DynamicTechForm.jsx` (Interactive Parameter Configuration)
3. `SpecificationViewer.jsx` (Canonical JSON Spec Viewer)
4. `SourceCodeViewer.jsx` (Code Explorer)
5. `BuildConsole.jsx` (Maven Build Logs and Auto-Fix)
6. `ArtifactInspector.jsx` (ESA validation and download)

## 3. Existing Working Pieces
- **Frontend Wizard Navigation:** `StepWizard.jsx` provides a clean UI flow.
- **AI Integration:** `AiRequirementParserService` parses text into `AdapterSpecification`.
- **Auto-Fix Loop:** AI auto-healing logic is already present, capturing Maven logs and regenerating code.
- **Code Generation & Templates:** Basic Freemarker templates (`metadata.xml.template`, `producer.java.template`, etc.) and `AdapterGeneratorService` exist and work for generic generation.

## 4. Existing Broken Pieces / Gaps Against Specification
- **Missing Firebase Implementation:** The Golden Reference Firebase plugin is completely missing. `TechnologyRegistry` contains mock HTTP/Kafka plugins but no Firebase. 
- **Missing Authentication & Security:** There is zero authentication in the backend. No Spring Security, no JWT, no Users.
- **Missing Database:** The backend has no PostgreSQL, JPA, or Flyway dependencies in `pom.xml`, and no entity models for Projects, Sessions, or Users. State is currently lost upon page reload.
- **Generic Core Leaks:** The `TechnologyPlugin` and `TemplateDefinition` abstraction is weakly defined; currently, `TechnologyRegistry` hardcodes `TechnologyDefinition` instances, but the code generator does not have a fully abstracted template resolution per technology (it uses generic templates).

## 5. Duplicate Implementations
Currently, there are no significant duplicate implementations. The backend is relatively lean and structured nicely in domain-driven packages (e.g., `controller`, `model`, `registry`, `service`).

## 6. Chatbot-Related Code
The frontend uses standard forms (`RequirementForm.jsx`) rather than a chat interface. No chatbot UI exists to be removed, which aligns with the product definition.

## 7. Firebase Implementation
**Current Status:** Not implemented.
**Required Action:** Build the `FirebaseFirestorePlugin` as a `TechnologyPlugin`, defining authentication (secure-store alias), parameters, and resolving Firebase-specific templates (`FirebaseComponentTemplate`, etc.). Add Firebase specific templates matching the Golden Reference. 

## 8. Generator Implementation
**Current Status:** `AdapterGeneratorService` resolves `AdapterSpecification` to generic Java/XML files.
**Required Action:** Refactor it to use a `TechnologyPlugin` contract to fetch technology-specific templates. The core must remain technology-agnostic. 

## 9. Build Implementation
**Current Status:** `MavenBuildWorkerService` runs `mvn clean install` in an isolated directory.
**Required Action:** Needs to integrate with real SAP ADK validation and ensure ESA output is correctly captured and reported to the frontend.

## 10. Database Implementation
**Current Status:** Non-existent. 
**Required Action:** Add Spring Data JPA, PostgreSQL driver, and Flyway. Create entities for `User`, `Project`, `GenerationSession`, `StepState`, `Build`, and `Artifact` to persist the 6-step state.

## 11. Authentication Implementation
**Current Status:** Non-existent.
**Required Action:** Implement Spring Security with JWT. Secure endpoints. Map Projects to Users. Update frontend to include Login/Registration.

## 12. Exact Repair Plan
1. **Database & Auth:** Add PostgreSQL/JPA/Security dependencies. Create data models and repositories. Secure backend and add frontend Auth pages.
2. **Project Persistence:** Bind the 6-step wizard state to `Project` entities in the database so progress can be resumed.
3. **Generalize Core:** Refine `TechnologyPlugin` and `TemplateDefinition` contracts. Update `AdapterGeneratorService` to resolve templates dynamically from the plugin. 
4. **Firebase Golden Reference:** Implement `FirebaseFirestorePlugin` in `TechnologyRegistry`. Add Firebase-specific templates (Dependencies, SecureStore usage, OSGi headers, Camel metadata) adhering to SAP Custom Adapter rules.
5. **ADK Validation & Artifacts:** Enhance `ArtifactValidatorService` and the build worker to parse SAP ADK validation results and expose the generated `.esa` securely to the frontend.
6. **End-to-End Test:** Run the Firebase Receiver flow and verify the generated ESA contains the correct classes, metadata, and dependencies.
