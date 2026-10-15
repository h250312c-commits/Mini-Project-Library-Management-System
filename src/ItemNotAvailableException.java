/** Signals that a requested library item is already on loan. */
public class ItemNotAvailableException extends Exception {
    /** Creates an exception with a description of why the item cannot be borrowed. */
    public ItemNotAvailableException(String message) {
        super(message);
    }
}
