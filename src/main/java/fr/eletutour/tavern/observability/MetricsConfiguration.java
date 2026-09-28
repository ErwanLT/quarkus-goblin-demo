package fr.eletutour.tavern.observability;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

/**
 * Publie l'histogramme des requêtes HTTP entrantes et sortantes, pour calculer les percentiles de latence dans
 * Prometheus / Grafana.
 */
public class MetricsConfiguration {

    @Produces
    @Singleton
    MeterFilter httpHistograms() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("http.server.requests") || id.getName().startsWith("http.client.requests")) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
