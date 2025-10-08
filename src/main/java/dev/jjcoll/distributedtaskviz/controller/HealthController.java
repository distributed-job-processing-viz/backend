package dev.jjcoll.distributedtaskviz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name="Health", description="Health check endpoints")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary="check API health", description = "Returns the health status of the API")
    public String getHealth() {
        return "Api up and running healthy.";
    }
}
