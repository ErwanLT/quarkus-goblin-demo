package fr.eletutour.tavern.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Un aventurier inscrit au registre de la taverne, avec sa bourse.
 */
@Entity
public class Adventurer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AdventurerClass adventurerClass;

    public int gold;

    public Adventurer() {
    }

    public Adventurer(String name, AdventurerClass adventurerClass, int gold) {
        this.name = name;
        this.adventurerClass = adventurerClass;
        this.gold = gold;
    }
}
