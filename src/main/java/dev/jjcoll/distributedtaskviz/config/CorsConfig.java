package dev.jjcoll.distributedtaskviz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for Cross-Origin Resource Sharing (CORS).
 * Allows the frontend application (running on a different port/origin) to make requests to this API.
 */
@Configuration
public class CorsConfig {

    /**
     * Configures CORS mappings to allow frontend access.
     *
     * Development setup:
     * - Frontend: http://localhost:5173 (Vite default port)
     * - Backend: http://localhost:8080 (Spring Boot default port)
     *
     * Production setup:
     * - Frontend: https://dist-viz.vercel.app
     * - Backend: https://backend-production-002c.up.railway.app
     *
     * @return WebMvcConfigurer with CORS configuration
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:5173",
                                "http://localhost:5174",
                                "https://dist-viz.vercel.app"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
