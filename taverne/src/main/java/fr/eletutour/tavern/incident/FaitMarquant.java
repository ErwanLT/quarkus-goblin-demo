package fr.eletutour.tavern.incident;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Un fait consigné pendant un incident : ce qui s'est réellement passé, mesuré, jamais interprété.
 */
@Entity
@Table(name = "fait_marquant")
public class FaitMarquant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    public Incident incident;

    @Column(nullable = false)
    public Instant horodatage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public TypeDeFait type;

    /** Clé de regroupement : source et type d'un assaut, ingrédient d'une livraison, statut d'une commande... */
    @Column(length = 200)
    public String cle;

    @Column(length = 1000)
    public String detail;

    /** Une mesure quand le fait en porte une : durée d'une commande, latence injectée... */
    public Long valeur;
}
