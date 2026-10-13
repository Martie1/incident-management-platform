package incident.management.platform.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties("app.seed")
record SeedProperties(Resource users, Resource incidents) {
}
