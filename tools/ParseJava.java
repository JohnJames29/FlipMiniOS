import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.util.*;
import java.nio.file.*;
import java.io.*;
/** Parse-only syntax check. No Android SDK required. */
public class ParseJava {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null);
        List<File> sources = new ArrayList<>();
        try (var stream = Files.walk(Path.of(args[0]))) {
            stream.filter(p -> p.toString().endsWith(".java") && !p.getFileName().toString().equals("ParseJava.java"))
                .forEach(p -> sources.add(p.toFile()));
        }
        JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                List.of("-proc:none"), null, files.getJavaFileObjectsFromFiles(sources));
        task.parse();
        long errors = diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).count();
        System.out.println("Parsed " + sources.size() + " Java sources: " + errors + " syntax errors");
        for (Diagnostic<?> d : diagnostics.getDiagnostics()) if (d.getKind() == Diagnostic.Kind.ERROR) System.out.println(d);
        if (errors != 0) System.exit(2);
    }
}
