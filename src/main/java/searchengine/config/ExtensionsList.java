package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.util.ArrayList;

@Data
@Configuration
@Component
@ConfigurationProperties("extensions-settings")
public class ExtensionsList {
    ArrayList<String> extensions = new ArrayList<>();
}
