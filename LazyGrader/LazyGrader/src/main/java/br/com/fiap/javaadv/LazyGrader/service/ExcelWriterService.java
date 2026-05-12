package br.com.fiap.javaadv.LazyGrader.service;

import br.com.fiap.javaadv.LazyGrader.model.Group;
import br.com.fiap.javaadv.LazyGrader.model.Member;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviço para escrita do Excel de resultados.
 */
@Slf4j
@Service
public class ExcelWriterService {

    public void writeResults(List<Group> groups, String outputPath) throws IOException {
        log.info("Escrevendo resultados em: {}", outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Avaliações");

            // Estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle groupStyle = createGroupStyle(workbook);
            CellStyle memberStyle = createMemberStyle(workbook);
            CellStyle gradeStyle = createGradeStyle(workbook);
            CellStyle feedbackStyle = createFeedbackStyle(workbook);

            // Cabeçalho
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Grupo", "Integrante", "RM",
                    "Repositório", "Vídeo",
                    "Nota IA", "Funcionalidade (0-3)",
                    "Qualidade Código (0-2)", "Arquitetura (0-2)",
                    "Documentação (0-2)", "Testes (0-1)",
                    "Feedback Detalhado", "Timestamp"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Group group : groups) {
                int groupStartRow = rowIndex;
                List<Member> members = group.getMembers();
                int memberCount = Math.max(members.size(), 1);

                for (int m = 0; m < memberCount; m++) {
                    Row row = sheet.createRow(rowIndex);

                    // Número do grupo (será mesclado)
                    Cell groupCell = row.createCell(0);
                    groupCell.setCellValue(group.getGroupNumber());
                    groupCell.setCellStyle(groupStyle);

                    // Integrante
                    if (m < members.size()) {
                        Member member = members.get(m);
                        createCell(row, 1, member.getName(), memberStyle);
                        createCell(row, 2, member.getRm(), memberStyle);
                    } else {
                        createCell(row, 1, "", memberStyle);
                        createCell(row, 2, "", memberStyle);
                    }

                    // URL repositório e vídeo (apenas na primeira linha do grupo)
                    if (m == 0) {
                        createCell(row, 3, group.getRepositoryUrl(), memberStyle);
                        createCell(row, 4, group.getVideoUrl(), memberStyle);

                        // Nota e rubrica
                        double aiGrade = group.getAiGrade() != null ? group.getAiGrade() : 0.0;
                        createCell(row, 5, aiGrade, gradeStyle);

                        if (group.getAiFeedback() != null) {
                            var rubric = extractRubricFromFeedback(group.getAiFeedback());
                            createCell(row, 6, rubric[0], memberStyle);
                            createCell(row, 7, rubric[1], memberStyle);
                            createCell(row, 8, rubric[2], memberStyle);
                            createCell(row, 9, rubric[3], memberStyle);
                            createCell(row, 10, rubric[4], memberStyle);
                        }

                        createCell(row, 11, group.getAiFeedback(), feedbackStyle);
                        createCell(row, 12,
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                                memberStyle);
                    } else {
                        // Células em branco para linhas extras de integrantes
                        for (int c = 3; c <= 12; c++) {
                            createCell(row, c, "", memberStyle);
                        }
                    }

                    rowIndex++;
                }

                // Mesclar células do grupo se houver mais de um membro
                if (memberCount > 1) {
                    mergeColumns(sheet, groupStartRow, rowIndex - 1, 0, 0);
                    mergeColumns(sheet, groupStartRow, rowIndex - 1, 3, 12);
                }
            }

            // Ajustar largura das colunas
            sheet.setColumnWidth(0, 2000);
            sheet.setColumnWidth(1, 7000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 12000);
            sheet.setColumnWidth(4, 12000);
            sheet.setColumnWidth(5, 3000);
            for (int i = 6; i <= 10; i++) sheet.setColumnWidth(i, 5000);
            sheet.setColumnWidth(11, 20000);
            sheet.setColumnWidth(12, 5000);

            // Congelar cabeçalho
            sheet.createFreezePane(0, 1);

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
        }

        log.info("Arquivo de resultados gravado com sucesso: {}", outputPath);
    }

    private void mergeColumns(XSSFSheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        if (firstRow < lastRow) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
        }
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Tenta extrair scores parciais do feedback gerado pela IA (heurístico simples).
     */
    private String[] extractRubricFromFeedback(String feedback) {
        // Retorna placeholders; em produção parsear o JSON estruturado da IA
        return new String[]{"-", "-", "-", "-", "-"};
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)31, (byte)73, (byte)125}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createGroupStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)220, (byte)230, (byte)241}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createMemberStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createGradeStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)198, (byte)224, (byte)180}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setDataFormat(wb.createDataFormat().getFormat("0.0"));
        return style;
    }

    private CellStyle createFeedbackStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderBottom(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }
}
