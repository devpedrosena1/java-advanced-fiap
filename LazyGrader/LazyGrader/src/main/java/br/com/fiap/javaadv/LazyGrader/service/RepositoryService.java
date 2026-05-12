package br.com.fiap.javaadv.LazyGrader.service;

import br.com.fiap.javaadv.LazyGrader.model.Group;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * Clona ou faz download de repositórios GitHub para avaliação.
 */
@Slf4j
@Service
public class RepositoryService {

    @Value("${LazyGrader.work-dir}")
    private String workDir;

    /**
     * Clona o repositório do grupo em um diretório local.
     * Retorna o caminho do diretório clonado.
     */
    public String cloneRepository(Group group) throws GitAPIException, IOException {
        String repoUrl = group.getRepositoryUrl();
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("URL do repositório não informada para o grupo " + group.getGroupNumber());
        }

        // Normalizar URL (remover .git no final se necessário)
        String normalizedUrl = normalizeGitHubUrl(repoUrl);

        // Diretório destino: workDir/grupo-N
        Path targetDir = Paths.get(workDir, "grupo-" + group.getGroupNumber());

        // Remover diretório anterior se existir
        if (Files.exists(targetDir)) {
            log.info("Removendo clone anterior do grupo {}", group.getGroupNumber());
            deleteDirectory(targetDir);
        }

        Files.createDirectories(targetDir);

        log.info("Clonando repositório do grupo {}: {}", group.getGroupNumber(), normalizedUrl);
        try {
            Git.cloneRepository()
                    .setURI(normalizedUrl)
                    .setDirectory(targetDir.toFile())
                    .setDepth(1)          // shallow clone para agilizar
                    .call()
                    .close();
        } catch (GitAPIException e) {
            log.error("Falha ao clonar via HTTPS, tentando com URL alternativa: {}", e.getMessage());
            // Tentar URL alternativa (ex: adicionar .git)
            String altUrl = normalizedUrl.endsWith(".git") ? normalizedUrl : normalizedUrl + ".git";
            Git.cloneRepository()
                    .setURI(altUrl)
                    .setDirectory(targetDir.toFile())
                    .setDepth(1)
                    .call()
                    .close();
        }

        log.info("Clone concluído em: {}", targetDir);
        return targetDir.toString();
    }

    /**
     * Detecta o tipo de projeto no diretório clonado.
     */
    public Group.ProjectType detectProjectType(String repoPath) {
        Path root = Paths.get(repoPath);

        // Java/Maven: pom.xml na raiz ou em subdiretório
        if (Files.exists(root.resolve("pom.xml"))) {
            return Group.ProjectType.JAVA_MAVEN;
        }

        // Python: requirements.txt, setup.py, main.py, ou arquivos .py
        if (Files.exists(root.resolve("requirements.txt"))
                || Files.exists(root.resolve("setup.py"))
                || Files.exists(root.resolve("main.py"))
                || hasPythonFiles(root)) {
            return Group.ProjectType.PYTHON;
        }

        // Verificar subdiretórios
        try {
            boolean hasMaven = Files.walk(root, 2)
                    .anyMatch(p -> p.getFileName().toString().equals("pom.xml"));
            if (hasMaven) return Group.ProjectType.JAVA_MAVEN;

            boolean hasPython = Files.walk(root, 2)
                    .anyMatch(p -> p.getFileName().toString().equals("requirements.txt"));
            if (hasPython) return Group.ProjectType.PYTHON;
        } catch (IOException e) {
            log.warn("Erro ao verificar tipo de projeto: {}", e.getMessage());
        }

        return Group.ProjectType.UNKNOWN;
    }

    /**
     * Lista os arquivos relevantes do projeto para análise.
     */
    public String collectProjectStructure(String repoPath, Group.ProjectType type) throws IOException {
        Path root = Paths.get(repoPath);
        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTRUTURA DO PROJETO ===\n");

        // Listar árvore de arquivos (ignorando .git, target, __pycache__, node_modules)
        Files.walk(root)
                .filter(p -> !isIgnored(p))
                .limit(200)
                .sorted()
                .forEach(p -> {
                    String relative = root.relativize(p).toString();
                    if (!relative.isEmpty()) {
                        sb.append(Files.isDirectory(p) ? "[DIR] " : "      ").append(relative).append("\n");
                    }
                });

        sb.append("\n=== CONTEÚDO DOS ARQUIVOS PRINCIPAIS ===\n");

        // Ler arquivos chave conforme tipo
        if (type == Group.ProjectType.JAVA_MAVEN) {
            readFileIfExists(root, "pom.xml", sb, 300);
            readJavaFiles(root, sb);
        } else if (type == Group.ProjectType.PYTHON) {
            readFileIfExists(root, "requirements.txt", sb, 100);
            readPythonFiles(root, sb);
        }

        // README sempre
        readFileIfExists(root, "README.md", sb, 200);
        readFileIfExists(root, "readme.md", sb, 200);

        return sb.toString();
    }

    // ---------- helpers privados ----------

    private String normalizeGitHubUrl(String url) {
        // Converter URL de browser para clone URL se necessário
        // https://github.com/user/repo → https://github.com/user/repo
        return url.trim().replaceAll("/$", "");
    }

    private boolean hasPythonFiles(Path root) {
        try {
            return Files.list(root).anyMatch(p -> p.toString().endsWith(".py"));
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isIgnored(Path path) {
        String s = path.toString();
        return s.contains("/.git") || s.contains("\\.git")
                || s.contains("/target/") || s.contains("\\target\\")
                || s.contains("/__pycache__") || s.contains("\\__pycache__")
                || s.contains("/node_modules") || s.contains("\\node_modules")
                || s.contains("/.idea") || s.contains("/.vscode");
    }

    private void readFileIfExists(Path root, String filename, StringBuilder sb, int maxLines) {
        Path file = root.resolve(filename);
        if (Files.exists(file)) {
            sb.append("\n--- ").append(filename).append(" ---\n");
            try {
                Files.lines(file).limit(maxLines).forEach(l -> sb.append(l).append("\n"));
            } catch (IOException e) {
                sb.append("[erro ao ler arquivo]\n");
            }
        }
    }

    private void readJavaFiles(Path root, StringBuilder sb) throws IOException {
        Files.walk(root)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !isIgnored(p))
                .limit(20)
                .forEach(p -> {
                    sb.append("\n--- ").append(root.relativize(p)).append(" ---\n");
                    try {
                        Files.lines(p).limit(150).forEach(l -> sb.append(l).append("\n"));
                        sb.append("[... truncado se maior]\n");
                    } catch (IOException e) {
                        sb.append("[erro ao ler]\n");
                    }
                });
    }

    private void readPythonFiles(Path root, StringBuilder sb) throws IOException {
        Files.walk(root)
                .filter(p -> p.toString().endsWith(".py"))
                .filter(p -> !isIgnored(p))
                .limit(20)
                .forEach(p -> {
                    sb.append("\n--- ").append(root.relativize(p)).append(" ---\n");
                    try {
                        Files.lines(p).limit(150).forEach(l -> sb.append(l).append("\n"));
                        sb.append("[... truncado se maior]\n");
                    } catch (IOException e) {
                        sb.append("[erro ao ler]\n");
                    }
                });
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
