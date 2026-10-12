public final class Book extends LibraryItem {
    private final String author;

    public Book(String id, String title, String author) {
        super(id, title);
        this.author = requireText(author, "Author");
    }

    public String getAuthor() {
        return author;
    }

    @Override
    public int loanPeriodDays() {
        return 21;
    }

    @Override
    public String toString() {
        return "Book{id='" + getId() + "', title='" + getTitle()
                + "', author='" + author + "', available=" + isAvailable() + "}";
    }
}
