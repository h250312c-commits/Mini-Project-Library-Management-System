/** Signals that a member has reached the maximum number of active loans. */
public class MemberLimitExceededException extends Exception {
    /** Creates an exception with a description of the member's loan-limit violation. */
    public MemberLimitExceededException(String message) {
        super(message);
    }
}
