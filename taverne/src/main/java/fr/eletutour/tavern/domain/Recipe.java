package fr.eletutour.tavern.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * Une page du grimoire : une recette, son prix de vente et les ingrédients à réunir (une unité de chacun par portion).
 */
@Entity
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String title;

    @Column(length = 1000)
    public String description;

    public int price;

    @ManyToMany
    @JoinTable(name = "RECIPE_INGREDIENT",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "ingredient_id"))
    public List<Ingredient> ingredients = new ArrayList<>();

    public Recipe() {
    }

    public Recipe(String title, String description, int price) {
        this.title = title;
        this.description = description;
        this.price = price;
    }
}
