package fr.eletutour.tavern.exception.business;

import java.util.List;
import java.util.Map;

/**
 * L'exception métier de la taverne : un {@link TavernError} et, au besoin, des informations complémentaires
 * restituées telles quelles au client.
 */
public class TavernException extends BusinessException {

    private final TavernError error;
    private final Map<String, Object> context;

    private TavernException(TavernError error, Map<String, Object> context, Object... detailArgs) {
        super(error.getStatus(), error.getTitle(), error.formatDetail(detailArgs));
        this.error = error;
        this.context = context;
    }

    public static TavernException of(TavernError error, Object... detailArgs) {
        return new TavernException(error, Map.of(), detailArgs);
    }

    public static TavernException outOfStock(String recipe, int portions, List<String> missing) {
        return new TavernException(TavernError.RUPTURE_DE_STOCK, Map.of("ingredientsManquants", missing), portions,
                recipe);
    }

    public static TavernException notEnoughGold(String adventurer, int price, int gold) {
        return new TavernException(TavernError.BOURSE_INSUFFISANTE, Map.of("prix", price, "bourse", gold), adventurer);
    }

    public TavernError getError() {
        return error;
    }

    public Map<String, Object> getContext() {
        return context;
    }
}
