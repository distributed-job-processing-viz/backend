# API Documentation with Springdoc and Swagger

## Overview

In this document I will go through how I use `springdoc` and Swagger with the OpenAPI specification to automatically create documentation for my endpoints. This automation will be the foundation for other automations that I will do in the future, specifically generating the frontend service layer using `swagger-typescript-api`.

### Springdoc

Springdoc is a library that automates the generation of API documentation for Spring Boot projects. It builds on the OpenAPI 3 specification and provides a user-friendly Swagger UI for testing and exploring endpoints.

**Key benefits:**
- Automatic API documentation from code annotations
- Interactive API testing through Swagger UI
- Machine-readable OpenAPI specification (JSON/YAML)
- Foundation for code generation tools

### Project Setup

#### 1. Adding the Dependency

First, I added the `springdoc-openapi-starter-webmvc-ui` dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.13</version>
</dependency>
```

This single dependency provides:
- OpenAPI 3 specification generation
- Swagger UI for interactive documentation
- Integration with Spring Boot 3.x

**[IMAGE 1: Screenshot of the dependency in pom.xml file]**

#### 2. Basic Configuration

In `application.properties`, I configured the basic Springdoc settings:

```properties
# OpenAPI Configuration
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=method
```

Configuration breakdown:
- `springdoc.api-docs.path`: Path where the OpenAPI JSON specification is served
- `springdoc.swagger-ui.path`: Path where Swagger UI is accessible
- `springdoc.swagger-ui.operations-sorter`: Sorts endpoints by HTTP method in the UI

**[IMAGE 2: Screenshot of application.properties with OpenAPI configuration highlighted]**

### API Metadata Configuration

To provide comprehensive API information, I created a dedicated configuration class `OpenApiConfig.java`:

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Distributed Task Visualization API")
                        .version("1.0.0")
                        .description("REST API for visualizing and managing distributed task execution across multiple nodes")
                        .contact(new Contact()
                                .name("Jordi Coll")
                                .email("contact@jjcoll.dev"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
```

This configuration defines:
- API title and description
- Version number
- Contact information
- License details

**[IMAGE 3: Screenshot of the OpenApiConfig.java class]**

### Documenting Controllers

#### Using @Tag and @Operation Annotations

Controllers are documented using Swagger annotations to provide clear, human-readable descriptions:

```java
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    @GetMapping
    @Operation(
        summary = "Check API health",
        description = "Returns the health status of the API"
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

Key annotations:
- `@Tag`: Groups related endpoints under a category
- `@Operation`: Documents individual endpoint with summary and description

**[IMAGE 4: Screenshot of the HealthController with annotations]**

### Documenting DTOs with @Schema

DTOs (Data Transfer Objects) are documented using `@Schema` annotations to describe request/response structures:

#### Request DTO Example

```java
@Schema(description = "Request object for submitting a new task")
public record TaskSubmissionRequestDTO(
    @Schema(
        description = "Name/description of the task to create",
        example = "Process user data",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Task name is required and cannot be blank")
    String name
) {}
```

#### Response DTO Example

```java
@Schema(description = "Response object containing task information")
public record TaskResponseDTO(
    @Schema(description = "Unique identifier of the task", example = "1")
    Long id,

    @Schema(description = "Name/description of the task", example = "Process user data")
    String name,

    @Schema(description = "Current status of the task", example = "PENDING")
    String status,

    @Schema(description = "Timestamp when the task was created",
            example = "2025-10-08T14:30:00")
    LocalDateTime createdAt,

    @Schema(description = "Timestamp when the task was completed",
            example = "2025-10-08T14:35:00",
            nullable = true)
    LocalDateTime completedAt
) {}
```

The `@Schema` annotation provides:
- Field descriptions
- Example values for testing
- Required/optional field indicators
- Nullable field specifications

**[IMAGE 5: Screenshot of TaskResponseDTO with @Schema annotations]**

### Accessing the Generated Documentation

Once the application is running, the documentation is accessible at multiple endpoints:

#### Swagger UI (Interactive Documentation)
- URL: `http://localhost:8080/swagger-ui.html`
- Interactive interface for testing endpoints
- Displays all documented endpoints grouped by tags
- Allows direct API calls from the browser

**[IMAGE 6: Screenshot of the Swagger UI homepage showing all endpoint groups]**

**[IMAGE 7: Screenshot of a specific endpoint in Swagger UI showing request/response schemas]**

#### OpenAPI Specification Files

The OpenAPI specification is automatically generated in multiple formats:

##### JSON Format
- URL: `http://localhost:8080/api-docs`
- Machine-readable JSON specification
- Used by code generation tools

**[IMAGE 8: Screenshot of the generated OpenAPI JSON specification]**

##### YAML Format
- URL: `http://localhost:8080/v3/api-docs.yaml`
- Human-readable YAML specification
- Alternative format for some tools

### Generating the OpenAPI Specification File

To enable code generation tools to consume the API specification, we export it to a static `swagger.json` file during the build process.

#### Adding the Maven Plugin

Add the `springdoc-openapi-maven-plugin` to `pom.xml`:

```xml
<plugin>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>1.4</version>
    <executions>
        <execution>
            <id>integration-test</id>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <apiDocsUrl>http://localhost:8080/api-docs</apiDocsUrl>
        <outputFileName>swagger.json</outputFileName>
        <outputDir>${project.build.directory}/generated-sources/openapi</outputDir>
    </configuration>
</plugin>
```

This plugin:
- Runs during the `integration-test` phase
- Starts the Spring Boot application temporarily
- Fetches the OpenAPI specification from `/api-docs`
- Saves it as `swagger.json` in the specified directory

#### Generating the File

Run the Maven command:

```bash
mvn clean install
```

The `swagger.json` file will be generated at:
```
target/generated-sources/openapi/swagger.json
```

**[IMAGE 9: Terminal screenshot showing the Maven build process generating swagger.json]**

**[IMAGE 10: Screenshot of the generated swagger.json file in the file explorer]**

### Best Practices

1. **Always document DTOs**: Use `@Schema` annotations with descriptions and examples
2. **Group related endpoints**: Use `@Tag` to organize controllers logically
3. **Provide clear descriptions**: Use `@Operation` to explain what each endpoint does
4. **Include examples**: Add realistic example values to help API consumers
5. **Version your API**: Update the version number in `OpenApiConfig` for breaking changes
6. **Generate during CI/CD**: Automate `swagger.json` generation in your build pipeline

### Future Automation: Frontend Code Generation

The `swagger.json` file serves as the foundation for generating the frontend service layer automatically using `swagger-typescript-api`. This tool will:

1. **Parse the OpenAPI specification**: Read `swagger.json` to understand all endpoints
2. **Generate TypeScript types**: Create interfaces for all DTOs automatically
3. **Generate API client code**: Create type-safe service functions for all endpoints
4. **Maintain synchronization**: Keep frontend and backend types in sync automatically

#### Example Workflow

```bash
# Generate TypeScript API client from swagger.json
npx swagger-typescript-api \
  -p ./backend/target/generated-sources/openapi/swagger.json \
  -o ./frontend/src/api \
  --modular \
  --axios
```

This will generate:
- `data-contracts.ts`: All DTO interfaces
- `Api.ts`: Type-safe API client with methods for all endpoints

**Benefits:**
- Eliminates manual API client code
- Ensures type safety between frontend and backend
- Automatically reflects API changes
- Reduces bugs from API contract mismatches

**[IMAGE 11: Diagram showing the flow from Spring Boot → swagger.json → swagger-typescript-api → Frontend TypeScript code]**

### Conclusion

By combining Springdoc annotations with proper configuration, we achieve:
- Automatic, always up-to-date API documentation
- Interactive testing capabilities through Swagger UI
- Machine-readable specifications for code generation
- Foundation for automated frontend-backend integration

This documentation-first approach reduces manual work, prevents API contract drift, and enables powerful automations like automatic TypeScript client generation.