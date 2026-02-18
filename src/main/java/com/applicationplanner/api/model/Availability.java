package com.applicationplanner.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "availability")
public class Availability {

    @Id
    @GeneratedValue
    @UuidGenerator
    UUID id;

    @Column(nullable = false) private int monHours;
    @Column(nullable = false) private int tueHours;
    @Column(nullable = false) private int wedHours;
    @Column(nullable = false) private int thuHours;
    @Column(nullable = false) private int friHours;
    @Column(nullable = false) private int satHours;
    @Column(nullable = false) private int sunHours;

    public Availability(){}

    private Availability defaultAvailability() {
        return getAvailability();
    }

    @NonNull
    public static Availability getAvailability() {
        Availability a = new Availability();
        a.setMonHours(2);
        a.setTueHours(2);
        a.setWedHours(2);
        a.setThuHours(2);
        a.setFriHours(2);
        a.setSatHours(4);
        a.setSunHours(2);
        return a;
    }
}
