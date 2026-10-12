import java.nio.file.Files;
import java.nio.file.Path;

public final class LibrarySmokeTest {
    private LibrarySmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path dataDirectory = Files.createTempDirectory("library-smoke-test");
        try {
            verifyLibraryBehavior(dataDirectory);
            System.out.println("Library smoke tests passed.");
        } finally {
            try (java.util.stream.Stream<Path> files = Files.list(dataDirectory)) {
                files.forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (java.io.IOException exception) {
                        throw new java.io.UncheckedIOException(exception);
                    }
                });
            }
            Files.delete(dataDirectory);
        }
    }

    private static void verifyLibraryBehavior(Path dataDirectory) throws Exception {
        Library library = new Library();
        Member member = new Member("M1", "A, Member", "member@example.com");
        library.registerMember(member);
        library.registerMember(new Member("M2", "Second Member", "second@example.com"));
        library.addItem(new Book("B1", "Java, the \"Complete\" Guide", "A. Author"));
        library.addItem(new Book("B2", "Java Basics", "B. Author"));
        library.addItem(new Book("B3", "Advanced Java", "C. Author"));
        library.addItem(new Book("B4", "Java Patterns", "D. Author"));
        Dvd dvd = new Dvd("D1", "Java on Film", 120);
        library.addItem(dvd);

        require(new Book("CHECK", "Check", "Author").loanPeriodDays() == 21,
                "Book loan period should be 21 days.");
        require(dvd.loanPeriodDays() == 7, "DVD loan period should be 7 days.");
        require(library.searchByTitle("JAVA").size() == 5,
                "Title search should be case-insensitive.");

        library.borrow("M1", "B1");
        library.borrow("M1", "B2");
        library.borrow("M1", "B3");
        expect(MemberLimitExceededException.class, () -> library.borrow("M1", "B4"));
        expect(ItemNotAvailableException.class, () -> library.borrow("M2", "B2"));

        library.giveBack("B1");
        Loan dvdLoan = library.borrow("M1", "D1");
        require(dvdLoan.getDueDate().equals(dvdLoan.getBorrowedOn().plusDays(7)),
                "DVD due date should use the DVD loan period.");

        CsvStore store = new CsvStore(dataDirectory);
        store.save(library);
        Library restored = new Library();
        store.loadInto(restored);

        require(restored.getItems().size() == 5, "All items should be restored.");
        require(restored.getMembers().size() == 2, "All members should be restored.");
        require(restored.getLoans().size() == 4, "Loan history should be restored.");
        require(restored.getItems().stream()
                        .anyMatch(item -> item.getTitle().equals("Java, the \"Complete\" Guide")),
                "Quoted and comma-containing CSV fields should round-trip.");
        require(!findItem(restored, "B2").isAvailable(),
                "An active loan should restore the item as unavailable.");
        require(findItem(restored, "B1").isAvailable(),
                "A returned item should restore as available.");
        require(restored.getMembers().stream()
                        .filter(candidate -> candidate.getId().equals("M1"))
                        .findFirst().orElseThrow().getActiveLoanCount() == 3,
                "Active member loans should be rebuilt.");

        restored.giveBack("D1");
        store.save(restored);
        Library afterReturn = new Library();
        store.loadInto(afterReturn);
        require(findItem(afterReturn, "D1").isAvailable(),
                "Returned state should persist across runs.");
        require(afterReturn.getLoans().size() == 4,
                "Returning an item should retain the loan history.");
    }

    private static LibraryItem findItem(Library library, String id) {
        return library.getItems().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends Exception> expected, CheckedAction action)
            throws Exception {
        try {
            action.run();
        } catch (Exception exception) {
            if (expected.isInstance(exception)) {
                return;
            }
            throw exception;
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
