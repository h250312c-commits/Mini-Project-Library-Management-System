import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Library library = new Library();
        CsvStore store = new CsvStore(Path.of("data"));
        try {
            store.loadInto(library);
        } catch (IOException exception) {
            System.err.println("Could not load library data: " + exception.getMessage());
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            new LibraryCli(library, scanner, System.out).run();
        }
        try {
            store.save(library);
            System.out.println("Library data saved.");
        } catch (IOException exception) {
            System.err.println("Could not save library data: " + exception.getMessage());
        }
    }
}
