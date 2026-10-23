package fr.eletutour.tavern.incident;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Un incident de la main courante : de la première dégradation au retour au calme.
 */
@Entity
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public Instant ouvertLe;

    /** Retour au calme, ou redémarrage de la taverne pour un incident interrompu. */
    public Instant closLe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public StatutIncident statut;

    /** Le premier signe de dégradation, celui qui a ouvert l'incident. */
    @Column(nullable = false, length = 500)
    public String declencheur;
}
