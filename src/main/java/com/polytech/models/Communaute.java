package com.polytech.models;

import javax.persistence.*;

/**
 * Created by E.Marouane on 02/05/2017.
 */

@Entity
@Table(name = "communaute")
public class Communaute {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "nom")
    private String nom;

    @Column(name = "responsable")
    private String responsable;

    @Column(name = "description")
    private String description;

    public Communaute() {
    }

    public Communaute(String nom, String responsable, String description) {
        this.nom = nom;
        this.description = description;
    }


    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsableID(String responsable) {
        this.responsable = responsable;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}