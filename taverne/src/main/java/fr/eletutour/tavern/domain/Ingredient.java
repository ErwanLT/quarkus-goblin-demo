package fr.eletutour.tavern.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Un ingrédient de la réserve : un nom, une unité de mesure et un coût en pièces d'or.
 */
@Entity
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String name;

    @Column(nullable = false)
    public String unit;

    public double cost;

    public Ingredient() {
    }

    public Ingredient(String name, String unit, double cost) {
        this.name = name;
        this.unit = unit;
        this.cost = cost;
    }
}
