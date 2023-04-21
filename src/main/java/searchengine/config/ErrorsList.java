package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Configuration
@Component
@ConfigurationProperties("errors-settings")
public class ErrorsList {
    private Map<String,String> errors;
}


