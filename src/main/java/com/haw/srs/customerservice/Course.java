package com.haw.srs.customerservice;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private Integer anzahlTeilnehmer;

    public Course(String name) {
        this.name = name;
        this.anzahlTeilnehmer = 0;
    }
}
