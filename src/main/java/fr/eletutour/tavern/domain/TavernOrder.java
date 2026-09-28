package fr.eletutour.tavern.domain;

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
 * Une commande passée au comptoir par un aventurier ({@code ORDER} étant un mot réservé SQL, la table s'appelle
 * {@code tavern_order}).
 */
@Entity
@Table(name = "tavern_order")
public class TavernOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    public Adventurer adventurer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    public Recipe recipe;

    public int quantity;

    public int total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrderStatus status;

    @Column(nullable = false)
    public Instant createdAt;
}
