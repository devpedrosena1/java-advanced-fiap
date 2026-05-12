package br.com.fiap.javaadv.LazyGrader.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa um integrante do grupo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    private String name;
    private String rm;  // Registro de Matrícula
}
