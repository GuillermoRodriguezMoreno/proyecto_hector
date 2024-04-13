package org.iesvdm.api_rest.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Invitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private long id;

    private String nombreInvitado;
    private String emailInvitado;
    private Boolean aceptado;
    private String intolerancias;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne
    private Boda boda;

    @OneToOne(mappedBy = "invitacion")
    private Invitado invitado;
}