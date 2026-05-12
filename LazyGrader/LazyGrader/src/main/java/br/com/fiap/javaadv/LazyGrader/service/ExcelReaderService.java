package br.com.fiap.javaadv.LazyGrader.service;

import br.com.fiap.javaadv.LazyGrader.model.Group;
import br.com.fiap.javaadv.LazyGrader.model.Member;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Serviço para leitura do arquivo Excel com os grupos.
 *
 * Layout esperado do Excel (uma linha por integrante):
 * Coluna A: Número do Grupo
 * Coluna B: Nome do Integrante
 * Coluna C: RM do Integrante
 * Coluna D: URL do Repositório (pode ser repetida por grupo)
 * Coluna E: URL do Vídeo (pode ser repetida por grupo)
 * Coluna F: Nota (opcional, pode estar em branco)
 * Coluna G: Comentários (opcional)
 */
@Slf4j
@Service
public class ExcelReaderService {

    /**
     * Lê o arquivo Excel e retorna a lista de grupos consolidados.
     */
    public List<Group> readGroups(String filePath) throws IOException {
        log.info("Lendo arquivo Excel: {}", filePath);

        Map<Integer, Group> groupMap = new LinkedHashMap<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;

            for (Row row : sheet) {
                // Pular cabeçalho
                if (firstRow) {
                    firstRow = false;
                    continue;
                }

                // Pular linhas vazias
                if (isRowEmpty(row)) continue;

                int groupNumber = (int) getNumericValue(row, 0);
                String memberName = getStringValue(row, 1);
                String memberRm = getStringValue(row, 2);
                String repoUrl = getStringValue(row, 3);
                String videoUrl = getStringValue(row, 4);
                Double grade = getOptionalNumericValue(row, 5);
                String comments = getStringValue(row, 6);

                // Consolidar membros no mesmo grupo
                Group group = groupMap.computeIfAbsent(groupNumber, n -> Group.builder()
                        .groupNumber(n)
                        .members(new ArrayList<>())
                        .repositoryUrl(repoUrl)
                        .videoUrl(videoUrl)
                        .grade(grade)
                        .comments(comments)
                        .build());

                // Atualizar URL se ainda não definida
                if (group.getRepositoryUrl() == null || group.getRepositoryUrl().isBlank()) {
                    group.setRepositoryUrl(repoUrl);
                }
                if (group.getVideoUrl() == null || group.getVideoUrl().isBlank()) {
                    group.setVideoUrl(videoUrl);
                }

                // Adicionar integrante
                if (!memberName.isBlank()) {
                    group.getMembers().add(Member.builder()
                            .name(memberName)
                            .rm(memberRm)
                            .build());
                }
            }
        }

        List<Group> groups = new ArrayList<>(groupMap.values());
        log.info("Total de grupos lidos: {}", groups.size());
        return groups;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = 0; i < 4; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getStringValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private double getNumericValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try { yield Double.parseDouble(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield 0; }
            }
            default -> 0;
        };
    }

    private Double getOptionalNumericValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try { yield Double.parseDouble(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }
}
