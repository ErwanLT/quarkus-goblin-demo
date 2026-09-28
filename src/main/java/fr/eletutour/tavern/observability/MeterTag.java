package fr.eletutour.tavern.observability;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Un tag statique ajouté aux métriques d'un {@link BusinessTimed}.
 */
@Retention(RUNTIME)
@Target({})
public @interface MeterTag {

    String key();

    String value();
}
