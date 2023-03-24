package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
@Data
@Configuration
@Component
@ConfigurationProperties("indexing-settings")
public class ExtensionsList {
    ArrayList<String> extensions = new ArrayList<>();
}
