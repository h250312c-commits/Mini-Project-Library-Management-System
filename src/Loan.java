import java.time.LocalDate;

/** Records a member's borrowing dates and return status for one library item. */
public final class Loan {
    private final String id;
    private final LibraryItem item;
    private final Member member;
    private final LocalDate borrowedOn;
    private final LocalDate dueDate;
    private LocalDate returnedOn;

    Loan(String id, LibraryItem item, Member member, LocalDate borrowedOn,
            LocalDate dueDate, LocalDate returnedOn) {
        this.id = LibraryItem.requireText(id, "Loan ID");
        if (item == null || member == null) {
            throw new IllegalArgumentException("A loan requires an item and a member.");
        }
        if (borrowedOn == null || dueDate == null || dueDate.isBefore(borrowedOn)) {
            throw new IllegalArgumentException("Loan dates are invalid.");
        }
        if (returnedOn != null && returnedOn.isBefore(borrowedOn)) {
            throw new IllegalArgumentException("Return date cannot precede the borrow date.");
        }
        this.item = item;
        this.member = member;
        this.borrowedOn = borrowedOn;
        this.dueDate = dueDate;
        this.returnedOn = returnedOn;
    }

    public String getId() {
        return id;
    }

    public LibraryItem getItem() {
        return item;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getBorrowedOn() {
        return borrowedOn;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public boolean isActive() {
        return returnedOn == null;
    }

    public boolean isOverdue() {
        // Returned loans are never considered overdue, even if they were returned late.
        return isActive() && dueDate.isBefore(LocalDate.now());
    }

    void markReturned(LocalDate date) {
        if (!isActive() || date == null || date.isBefore(borrowedOn)) {
            throw new IllegalStateException("Loan cannot be marked returned.");
        }
        returnedOn = date;
    }

    @Override
    public String toString() {
        return "Loan{id='" + id + "', item='" + item.getTitle() + "', member='"
                + member.getName() + "', borrowedOn=" + borrowedOn
                + ", dueDate=" + dueDate + ", returnedOn=" + returnedOn + "}";
    }
}
