import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CsvStore {
    private static final String ITEMS_FILE = "items.csv";
    private static final String MEMBERS_FILE = "members.csv";
    private static final String LOANS_FILE = "loans.csv";
    private final Path directory;

    public CsvStore(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Data directory is required.");
        }
        this.directory = directory;
    }

    public void loadInto(Library library) throws IOException {
        if (library == null) {
            throw new IllegalArgumentException("Library is required.");
        }
        if (!Files.exists(directory)) {
            return;
        }
        for (List<String> row : readRows(ITEMS_FILE, "type,id,title,detail")) {
            requireColumns(row, 4, ITEMS_FILE);
            LibraryItem item;
            try {
                if ("BOOK".equals(row.get(0))) {
                    item = new Book(row.get(1), row.get(2), row.get(3));
                } else if ("DVD".equals(row.get(0))) {
                    item = new Dvd(row.get(1), row.get(2), Integer.parseInt(row.get(3)));
                } else {
                    throw new IllegalArgumentException("Unknown item type: " + row.get(0));
                }
                library.addItem(item);
            } catch (IllegalArgumentException exception) {
                throw invalidRow(ITEMS_FILE, exception);
            }
        }
        for (List<String> row : readRows(MEMBERS_FILE, "id,name,email")) {
            requireColumns(row, 3, MEMBERS_FILE);
            try {
                library.registerMember(new Member(row.get(0), row.get(1), row.get(2)));
            } catch (IllegalArgumentException exception) {
                throw invalidRow(MEMBERS_FILE, exception);
            }
        }
        for (List<String> row : readRows(
                LOANS_FILE, "id,memberId,itemId,borrowedOn,dueDate,returnedOn")) {
            requireColumns(row, 6, LOANS_FILE);
            try {
                LocalDate returnedOn = row.get(5).isEmpty()
                        ? null : LocalDate.parse(row.get(5));
                library.restoreLoan(row.get(0), row.get(1), row.get(2),
                        LocalDate.parse(row.get(3)), LocalDate.parse(row.get(4)), returnedOn);
            } catch (DateTimeException | IllegalArgumentException
                    | MemberLimitExceededException exception) {
                throw invalidRow(LOANS_FILE, exception);
            }
        }
    }

    public void save(Library library) throws IOException {
        if (library == null) {
            throw new IllegalArgumentException("Library is required.");
        }
        Files.createDirectories(directory);

        List<List<String>> itemRows = new ArrayList<>();
        itemRows.add(Arrays.asList("type", "id", "title", "detail"));
        for (LibraryItem item : library.getItems()) {
            if (item instanceof Book) {
                itemRows.add(Arrays.asList("BOOK", item.getId(), item.getTitle(),
                        ((Book) item).getAuthor()));
            } else if (item instanceof Dvd) {
                itemRows.add(Arrays.asList("DVD", item.getId(), item.getTitle(),
                        Integer.toString(((Dvd) item).getRuntimeMinutes())));
            } else {
                throw new IOException("Unsupported library item type: "
                        + item.getClass().getName());
            }
        }

        List<List<String>> memberRows = new ArrayList<>();
        memberRows.add(Arrays.asList("id", "name", "email"));
        for (Member member : library.getMembers()) {
            memberRows.add(Arrays.asList(member.getId(), member.getName(), member.getEmail()));
        }

        List<List<String>> loanRows = new ArrayList<>();
        loanRows.add(Arrays.asList(
                "id", "memberId", "itemId", "borrowedOn", "dueDate", "returnedOn"));
        for (Loan loan : library.getLoans()) {
            loanRows.add(Arrays.asList(
                    loan.getId(),
                    loan.getMember().getId(),
                    loan.getItem().getId(),
                    loan.getBorrowedOn().toString(),
                    loan.getDueDate().toString(),
                    loan.getReturnedOn() == null ? "" : loan.getReturnedOn().toString()));
        }

        writeRows(ITEMS_FILE, itemRows);
        writeRows(MEMBERS_FILE, memberRows);
        writeRows(LOANS_FILE, loanRows);
    }

    private List<List<String>> readRows(String filename, String header) throws IOException {
        Path file = directory.resolve(filename);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        boolean validHeader;
        try {
            validHeader = !lines.isEmpty()
                    && parseLine(lines.get(0)).equals(parseLine(header));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid CSV header in " + file, exception);
        }
        if (!validHeader) {
            throw new IOException("Invalid or missing CSV header in " + file);
        }
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isEmpty()) {
                try {
                    rows.add(parseLine(lines.get(i)));
                } catch (IllegalArgumentException exception) {
                    throw new IOException("Invalid CSV row " + (i + 1) + " in " + file,
                            exception);
                }
            }
        }
        return rows;
    }

    private void writeRows(String filename, List<List<String>> rows) throws IOException {
        Path target = directory.resolve(filename);
        Path temporary = directory.resolve(filename + ".tmp");
        List<String> lines = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> escaped = new ArrayList<>();
            for (String value : row) {
                escaped.add(escape(value));
            }
            lines.add(String.join(",", escaped));
        }
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String escape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (quoted) {
                if (character == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(character);
                }
            } else if (character == ',' ) {
                fields.add(field.toString());
                field.setLength(0);
                closedQuote = false;
            } else if (character == '"' && field.length() == 0) {
                quoted = true;
            } else if (closedQuote) {
                throw new IllegalArgumentException("Unexpected text after quoted CSV field.");
            } else if (character == '"') {
                throw new IllegalArgumentException("Unexpected quote in CSV field.");
            } else {
                field.append(character);
            }
            if (!quoted && character == '"') {
                closedQuote = true;
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed quoted CSV field.");
        }
        fields.add(field.toString());
        return fields;
    }

    private static void requireColumns(List<String> row, int count, String file)
            throws IOException {
        if (row.size() != count) {
            throw new IOException("Expected " + count + " columns in " + file
                    + " but found " + row.size() + ".");
        }
    }

    private static IOException invalidRow(String filename, Exception cause) {
        return new IOException("Invalid data in " + filename + ": " + cause.getMessage(), cause);
    }
}
