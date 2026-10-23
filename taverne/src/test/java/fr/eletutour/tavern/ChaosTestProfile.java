package fr.eletutour.tavern;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Autorise Quarkus Goblin à attaquer les tests de cette classe (il reste inactif dans les autres).
 */
public class ChaosTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.goblin.test.enabled", "true");
    }
}
