package fr.eletutour.tavern.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * Une étagère de la cave : la quantité disponible d'un ingrédient.
 */
@Entity
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "ingredient_id", unique = true)
    public Ingredient ingredient;

    public int quantity;

    public Stock() {
    }

    public Stock(Ingredient ingredient, int quantity) {
        this.ingredient = ingredient;
        this.quantity = quantity;
    }
}
