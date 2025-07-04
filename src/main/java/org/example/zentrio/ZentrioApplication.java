package org.example.zentrio;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@OpenAPIDefinition(
        info = @Info(
                title = "Zentrio Management System KSHRD",
                version = "1.0",
                description = "API documentation for the managing tasks application"
        ),
        servers = {
                @Server(url = "/",description = "Root Server"),
                @Server(url = "https://team.zentrio.pro",description = "Production Server"),
                @Server(url = "http://localhost:8080",description = "Local Server")
        }
)

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        in = SecuritySchemeIn.HEADER
)

@SpringBootApplication
public class ZentrioApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZentrioApplication.class, args);
    }

}
