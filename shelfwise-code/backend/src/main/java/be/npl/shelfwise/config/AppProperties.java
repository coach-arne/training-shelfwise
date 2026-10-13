package be.npl.shelfwise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Deployment settings (shelfwise.*). */
@ConfigurationProperties("shelfwise")
public record AppProperties(
        @DefaultValue("false") boolean trainingMode,
        @DefaultValue("Europe/Brussels") String timeZone,
        @DefaultValue("http://localhost:4200/portal") String portalUrl,
        String jwtSecret,
        @DefaultValue("12") int jwtExpiryHours,
        @DefaultValue("true") boolean seedOnEmpty,
        @DefaultValue("true") boolean schedulerEnabled,
        @DefaultValue("0 0 22 * * *") String dailyProcessingCron) {
}
