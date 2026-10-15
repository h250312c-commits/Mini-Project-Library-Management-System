/** Defines the operations supported by items that can be loaned and returned. */
public interface Borrowable {
    /** Starts a loan for a member, subject to availability and member limits. */
    Loan borrow(Member member)
            throws ItemNotAvailableException, MemberLimitExceededException;

    /** Returns the currently loaned item. */
    void giveBack();
}
