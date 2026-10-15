/** Shared identity, availability, and loan handling for books and DVDs. */
public abstract class LibraryItem implements Borrowable {
    private final String id;
    private final String title;
    private boolean available = true;
    private Loan activeLoan;

    protected LibraryItem(String id, String title) {
        this.id = requireText(id, "Item ID");
        this.title = requireText(title, "Title");
    }

    public abstract int loanPeriodDays();

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public Loan borrow(Member member)
            throws ItemNotAvailableException, MemberLimitExceededException {
        if (member == null) {
            throw new IllegalArgumentException("Member is required.");
        }
        if (!available) {
            throw new ItemNotAvailableException("Item is already on loan: " + title);
        }

        java.time.LocalDate borrowedOn = java.time.LocalDate.now();
        Loan loan = new Loan(
                java.util.UUID.randomUUID().toString(),
                this,
                member,
                borrowedOn,
                borrowedOn.plusDays(loanPeriodDays()),
                null);
        // Add to the member first so a limit failure leaves the item available.
        member.addLoan(loan);
        activeLoan = loan;
        available = false;
        return loan;
    }

    @Override
    public void giveBack() {
        if (available || activeLoan == null) {
            throw new IllegalStateException("Item is not currently on loan: " + title);
        }
        activeLoan.getMember().removeLoan(activeLoan);
        activeLoan.markReturned(java.time.LocalDate.now());
        activeLoan = null;
        available = true;
    }

    void restoreLoan(Loan loan) throws MemberLimitExceededException {
        if (!available) {
            throw new IllegalArgumentException("Multiple active loans for item: " + id);
        }
        loan.getMember().addLoan(loan);
        activeLoan = loan;
        available = false;
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        String trimmed = value.trim();
        // Line breaks are disallowed because these values are stored in row-based CSV files.
        if (trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(fieldName + " cannot contain line breaks.");
        }
        return trimmed;
    }
}
