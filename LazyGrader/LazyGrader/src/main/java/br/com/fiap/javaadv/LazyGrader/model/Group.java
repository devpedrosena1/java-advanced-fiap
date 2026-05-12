package br.com.fiap.javaadv.LazyGrader.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Representa um grupo de alunos lido do Excel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    private int groupNumber;
    private List<Member> members;
    private String repositoryUrl;
    private String videoUrl;
    private Double grade;           // nota atual (pode estar vazia no Excel)
    private String comments;        // comentários atuais
    // Preenchido após avaliação pela IA
    private Double aiGrade;
    private String aiFeedback;
    private String localRepoPath;
    private ProjectType projectType;

    public enum ProjectType {
        JAVA_MAVEN, PYTHON, UNKNOWN
    }
}
