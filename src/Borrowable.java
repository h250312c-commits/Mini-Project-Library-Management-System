public interface Borrowable {
    Loan borrow(Member member)
            throws ItemNotAvailableException, MemberLimitExceededException;

    void giveBack();
}
