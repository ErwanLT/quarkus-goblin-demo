package fr.eletutour.tavern.observability;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Le contrat d'une métrique métier : le code métier dit <em>quoi</em> mesurer, {@link BusinessTimedInterceptor} sait
 * <em>comment</em>. Génère un compteur {@code <value>.count} et un timer {@code <value>} avec histogramme.
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target({ METHOD, TYPE })
public @interface BusinessTimed {

    @Nonbinding
    String value();

    @Nonbinding
    String description() default "";

    @Nonbinding
    MeterTag[] tags() default {};
}
