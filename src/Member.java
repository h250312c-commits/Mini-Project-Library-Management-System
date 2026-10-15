import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** A registered library member and the member's currently active loans. */
public final class Member {
    private static final int MAX_ACTIVE_LOANS = 3;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String id;
    private final String name;
    private final String email;
    private final List<Loan> activeLoans = new ArrayList<>();

    public Member(String id, String name, String email) {
        this.id = LibraryItem.requireText(id, "Member ID");
        this.name = LibraryItem.requireText(name, "Member name");
        this.email = LibraryItem.requireText(email, "Email");
        if (!EMAIL_PATTERN.matcher(this.email).matches()) {
            throw new IllegalArgumentException("Email address is invalid.");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public List<Loan> getActiveLoans() {
        return Collections.unmodifiableList(new ArrayList<>(activeLoans));
    }

    public int getActiveLoanCount() {
        return activeLoans.size();
    }

    void addLoan(Loan loan) throws MemberLimitExceededException {
        // Enforce the borrowing cap before changing the active-loan list.
        if (activeLoans.size() >= MAX_ACTIVE_LOANS) {
            throw new MemberLimitExceededException(
                    "Member " + name + " already has the maximum of "
                            + MAX_ACTIVE_LOANS + " active loans.");
        }
        activeLoans.add(loan);
    }

    void removeLoan(Loan loan) {
        if (!activeLoans.remove(loan)) {
            throw new IllegalStateException("Loan is not active for member: " + id);
        }
    }

    @Override
    public String toString() {
        return "Member{id='" + id + "', name='" + name + "', email='"
                + email + "', activeLoans=" + activeLoans.size() + "}";
    }
}
