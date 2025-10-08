package dev.jjcoll.distributedtaskviz.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI (Swagger) documentation.
 * This class is marked with @Configuration to indicate it contains bean definitions
 * that will be processed by the Spring container.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates and configures the OpenAPI bean for API documentation.
     * The @Bean annotation tells Spring to manage this method's return value as a bean
     * in the application context, making it available for dependency injection.
     *
     * @return OpenAPI object containing API metadata and configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Configure the API information section
                .info(new Info()
                        // Set the title that appears in the Swagger UI
                        .title("Distributed Task Visualization API")
                        // Set the version of the API
                        .version("1.0.0")
                        // Provide a description of what the API does
                        .description("REST API for visualizing and managing distributed task execution across multiple nodes")
                        // Add contact information for API support
                        .contact(new Contact()
                                .name("Jordi Coll")
                                .email("contact@jjcoll.dev"))
                        // Specify the license under which the API is available
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
