import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/** Handles menu input and presents library operations in a console session. */
public final class LibraryCli {
    private final Library library;
    private final Scanner scanner;
    private final PrintStream output;

    public LibraryCli(Library library, Scanner scanner, PrintStream output) {
        if (library == null || scanner == null || output == null) {
            throw new IllegalArgumentException("Library, input, and output are required.");
        }
        this.library = library;
        this.scanner = scanner;
        this.output = output;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            if (!scanner.hasNextLine()) {
                output.println("No more input available. Exiting library system.");
                break;
            }

            String choice = scanner.nextLine().trim();
            try {
                // Keep the menu active after recoverable input and library-operation errors.
                switch (choice) {
                    case "1":
                        addItem();
                        break;
                    case "2":
                        registerMember();
                        break;
                    case "3":
                        borrowItem();
                        break;
                    case "4":
                        returnItem();
                        break;
                    case "5":
                        showOverdue();
                        break;
                    case "6":
                        searchByTitle();
                        break;
                    case "7":
                        listItems();
                        break;
                    case "8":
                        listMembers();
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        output.println("Choose a listed menu option.");
                }
            } catch (ItemNotAvailableException | MemberLimitExceededException
                    | IllegalArgumentException | IllegalStateException exception) {
                output.println("Error: " + exception.getMessage());
            }
        }
    }

    private void printMenu() {
        output.println();
        output.println("=== Library Management ===");
        output.println("1. Add item");
        output.println("2. Register member");
        output.println("3. Borrow item");
        output.println("4. Return item");
        output.println("5. List overdue items");
        output.println("6. Search by title");
        output.println("7. List all items");
        output.println("8. List all members");
        output.println("0. Save and exit");
        output.print("Select: ");
    }

    private void addItem() {
        String type = readLine("Type (BOOK/DVD): ").trim().toUpperCase();
        String id = readLine("Item ID: ");
        String title = readLine("Title: ");
        LibraryItem item;
        if ("BOOK".equals(type)) {
            item = new Book(id, title, readLine("Author: "));
        } else if ("DVD".equals(type)) {
            item = new Dvd(id, title, readPositiveInteger("Runtime in minutes: "));
        } else {
            throw new IllegalArgumentException("Item type must be BOOK or DVD.");
        }
        library.addItem(item);
        output.println("Added: " + item);
    }

    private void registerMember() {
        Member member = new Member(
                readLine("Member ID: "),
                readLine("Name: "),
                readLine("Email: "));
        library.registerMember(member);
        output.println("Registered: " + member);
    }

    private void borrowItem() throws ItemNotAvailableException, MemberLimitExceededException {
        Loan loan = library.borrow(readLine("Member ID: "), readLine("Item ID: "));
        output.println("Borrowed. Due date: " + loan.getDueDate());
    }

    private void returnItem() {
        library.giveBack(readLine("Item ID: "));
        output.println("Item returned.");
    }

    private void showOverdue() {
        List<Loan> overdue = library.listOverdueLoans();
        if (overdue.isEmpty()) {
            output.println("No overdue items.");
            return;
        }
        overdue.forEach(loan -> output.println(loan.getItem().getTitle()
                + " (item " + loan.getItem().getId() + "), member "
                + loan.getMember().getName() + ", due " + loan.getDueDate()));
    }

    private void searchByTitle() {
        List<LibraryItem> results = library.searchByTitle(readLine("Title search: "));
        if (results.isEmpty()) {
            output.println("No matching items.");
            return;
        }
        results.forEach(output::println);
    }

    private void listItems() {
        library.getItems().forEach(output::println);
    }

    private void listMembers() {
        library.getMembers().forEach(output::println);
    }

    private String readLine(String prompt) {
        output.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("Input ended unexpectedly.");
        }
        return scanner.nextLine();
    }

    private int readPositiveInteger(String prompt) {
        String value = readLine(prompt);
        try {
            int number = Integer.parseInt(value.trim());
            if (number <= 0) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a positive whole number.");
        }
    }
}
