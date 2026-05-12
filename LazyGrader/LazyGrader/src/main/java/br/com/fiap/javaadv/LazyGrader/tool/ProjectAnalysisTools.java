package br.com.fiap.javaadv.LazyGrader.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class ProjectAnalysisTools {

    @Tool(description = "Lê o conteúdo de um arquivo no repositório do aluno.")
    public String readFile(@ToolParam(description = "Caminho absoluto do arquivo a ser lido") String filePath) {
        try {
            Path path = Path.of(filePath);
            if ( !Files.exists(path))
                return "ERRO: Arquivo não encontrado - " + filePath;
            if ( !Files.isDirectory(path))
                return "ERRO: O caminho é um diretório, não um arquivo - " + filePath;

            long size = Files.size(path);
            if (size > 100_000)
                return "Arquivo muito grande: " + size + " bytes - " + filePath;

            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
