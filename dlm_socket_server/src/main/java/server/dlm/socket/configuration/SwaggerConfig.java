package server.dlm.socket.configuration;// Swagger Configuration (Springdoc)
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(title = "DLM Socket API", version = "1.0", description = "API for managing DLM devices")
)
public class SwaggerConfig {
}