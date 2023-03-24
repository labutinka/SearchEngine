package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Configuration
@Component
@ConfigurationProperties("jsoup-settings")
public class JsoupSettings {
    private String referrer;
    private String userAgent;

}
