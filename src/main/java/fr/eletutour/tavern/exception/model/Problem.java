package fr.eletutour.tavern.exception.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * La carte des pièges : une réponse d'erreur unique au format RFC 9457 ({@code application/problem+json}). Les
 * membres d'extension ({@code code}, {@code traceId}, détails métier) sont sérialisés au premier niveau.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Problem", description = "Erreur au format RFC 9457 (application/problem+json)")
public class Problem {

    @Schema(description = "Type du problème", examples = "urn:falling-whale:problem:recette-introuvable")
    private String type;

    @Schema(description = "Résumé lisible du problème", examples = "Ressource introuvable")
    private String title;

    @Schema(description = "Code HTTP", examples = "404")
    private int status;

    @Schema(description = "Explication propre à cette occurrence", examples = "La recette 42 n'existe pas dans le grimoire.")
    private String detail;

    @Schema(description = "URI de la requête en erreur", examples = "http://localhost:8080/grimoire/recettes/42")
    private String instance;

    @Schema(description = "Horodatage UTC de l'erreur", examples = "2026-09-26T12:00:00Z")
    private String timestamp;

    private final Map<String, Object> additional = new LinkedHashMap<>();

    private Problem() {
    }

    public static Problem of(int status, String title) {
        Problem p = new Problem();
        p.status = status;
        p.title = title;
        return p;
    }

    public Problem type(String type) {
        this.type = type;
        return this;
    }

    public Problem detail(String detail) {
        this.detail = detail;
        return this;
    }

    public Problem instance(String instance) {
        this.instance = instance;
        return this;
    }

    public Problem timestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Problem additional(Map<String, Object> additional) {
        this.additional.putAll(additional);
        return this;
    }

    public Problem with(String key, Object value) {
        if (value != null) {
            this.additional.put(key, value);
        }
        return this;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public String getInstance() {
        return instance;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @JsonAnyGetter
    @Schema(hidden = true)
    public Map<String, Object> getAdditional() {
        return additional;
    }
}
