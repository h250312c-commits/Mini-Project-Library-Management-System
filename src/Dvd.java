/** A library item with a runtime and a seven-day loan period. */
public final class Dvd extends LibraryItem {
    private final int runtimeMinutes;

    public Dvd(String id, String title, int runtimeMinutes) {
        super(id, title);
        if (runtimeMinutes <= 0) {
            throw new IllegalArgumentException("Runtime must be greater than zero.");
        }
        this.runtimeMinutes = runtimeMinutes;
    }

    public int getRuntimeMinutes() {
        return runtimeMinutes;
    }

    @Override
    public int loanPeriodDays() {
        return 7;
    }

    @Override
    public String toString() {
        return "Dvd{id='" + getId() + "', title='" + getTitle()
                + "', runtimeMinutes=" + runtimeMinutes
                + ", available=" + isAvailable() + "}";
    }
}
