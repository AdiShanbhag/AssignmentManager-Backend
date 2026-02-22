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
    private UUID id;

    @Column(name = "mon_hours", nullable = false)
    private int monHours;

    @Column(name = "tue_hours", nullable = false)
    private int tueHours;

    @Column(name = "wed_hours", nullable = false)
    private int wedHours;

    @Column(name = "thu_hours", nullable = false)
    private int thuHours;

    @Column(name = "fri_hours", nullable = false)
    private int friHours;

    @Column(name = "sat_hours", nullable = false)
    private int satHours;

    @Column(name = "sun_hours", nullable = false)
    private int sunHours;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public Availability() {}

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