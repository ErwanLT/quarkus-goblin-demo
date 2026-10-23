package fr.eletutour.tavern.observability;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

/**
 * Standardise les métriques métier : un compteur d'appels, un timer avec histogramme et un tag {@code outcome}
 * ({@code success} / {@code failure}) pour suivre le taux d'échec d'une opération métier.
 */
@BusinessTimed("")
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class BusinessTimedInterceptor {

    private static final String COUNTER_SUFFIX = ".count";

    @Inject
    MeterRegistry meterRegistry;

    @AroundInvoke
    Object track(InvocationContext context) throws Exception {
        BusinessTimed timed = resolveAnnotation(context);
        if (timed == null || timed.value().isBlank()) {
            return context.proceed();
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            return context.proceed();
        } catch (Exception e) {
            outcome = "failure";
            throw e;
        } finally {
            List<Tag> tags = toTags(timed.tags());
            tags.add(Tag.of("outcome", outcome));
            Counter.builder(timed.value() + COUNTER_SUFFIX)
                    .description(description(timed.description(), "Number of calls"))
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
            sample.stop(Timer.builder(timed.value())
                    .description(description(timed.description(), "Execution time"))
                    .publishPercentileHistogram()
                    .tags(tags)
                    .register(meterRegistry));
        }
    }

    private BusinessTimed resolveAnnotation(InvocationContext context) {
        BusinessTimed timed = context.getMethod().getAnnotation(BusinessTimed.class);
        if (timed != null) {
            return timed;
        }
        return context.getMethod().getDeclaringClass().getAnnotation(BusinessTimed.class);
    }

    private List<Tag> toTags(MeterTag[] meterTags) {
        List<Tag> tags = new ArrayList<>();
        for (MeterTag meterTag : meterTags) {
            tags.add(Tag.of(meterTag.key(), meterTag.value()));
        }
        return tags;
    }

    private String description(String provided, String fallback) {
        return (provided == null || provided.isBlank()) ? fallback : provided;
    }
}
