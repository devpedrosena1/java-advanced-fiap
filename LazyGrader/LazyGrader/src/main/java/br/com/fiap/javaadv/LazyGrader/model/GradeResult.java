package br.com.fiap.javaadv.LazyGrader.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado da avaliação de um grupo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResult {

    private int groupNumber;
    private double grade;           // 0.0 a 10.0
    private String feedback;        // feedback detalhado
    private String rubricSummary;   // resumo dos critérios avaliados
    private boolean evaluated;
    private String errorMessage;    // se houve erro durante a avaliação

    // Detalhamento por critério da rubrica
    private RubricDetail rubricDetail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RubricDetail {
        private double functionalityScore;   // 0-3
        private double codeQualityScore;     // 0-2
        private double architectureScore;   // 0-2
        private double documentationScore;  // 0-2
        private double testScore;           // 0-1
        private String observations;
    }
}