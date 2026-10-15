import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Coordinates library items, members, and the loans created between them. */
public final class Library {
    private final Map<String, LibraryItem> items = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, Loan> loans = new HashMap<>();

    public void addItem(LibraryItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item is required.");
        }
        if (items.putIfAbsent(item.getId(), item) != null) {
            throw new IllegalArgumentException("An item with this ID already exists.");
        }
    }

    public void registerMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member is required.");
        }
        if (members.putIfAbsent(member.getId(), member) != null) {
            throw new IllegalArgumentException("A member with this ID already exists.");
        }
    }

    public Loan borrow(String memberId, String itemId)
            throws ItemNotAvailableException, MemberLimitExceededException {
        Member member = members.get(memberId);
        LibraryItem item = items.get(itemId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + memberId);
        }
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }
        Loan loan = item.borrow(member);
        loans.put(loan.getId(), loan);
        return loan;
    }

    public void giveBack(String itemId) {
        LibraryItem item = items.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }
        item.giveBack();
    }

    public List<Loan> listOverdueLoans() {
        return loans.values().stream()
                .filter(Loan::isOverdue)
                .sorted(Comparator.comparing(Loan::getDueDate))
                .collect(Collectors.toList());
    }

    public List<LibraryItem> searchByTitle(String text) {
        String query = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return items.values().stream()
                .filter(item -> item.getTitle().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(LibraryItem::getTitle,
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<LibraryItem> getItems() {
        return items.values().stream()
                .sorted(Comparator.comparing(LibraryItem::getId))
                .collect(Collectors.toList());
    }

    public List<Member> getMembers() {
        return members.values().stream()
                .sorted(Comparator.comparing(Member::getId))
                .collect(Collectors.toList());
    }

    public List<Loan> getLoans() {
        return loans.values().stream()
                .sorted(Comparator.comparing(Loan::getBorrowedOn)
                        .thenComparing(Loan::getId))
                .collect(Collectors.toList());
    }

    void restoreLoan(String id, String memberId, String itemId,
            LocalDate borrowedOn, LocalDate dueDate, LocalDate returnedOn)
            throws MemberLimitExceededException {
        Member member = members.get(memberId);
        LibraryItem item = items.get(itemId);
        if (member == null || item == null) {
            throw new IllegalArgumentException("Loan refers to an unknown member or item.");
        }
        Loan loan = new Loan(id, item, member, borrowedOn, dueDate, returnedOn);
        if (loans.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate loan ID: " + id);
        }
        if (loan.isActive()) {
            // Rebuild the item's active-loan state when loading persisted records.
            item.restoreLoan(loan);
        }
        loans.put(id, loan);
    }
}
