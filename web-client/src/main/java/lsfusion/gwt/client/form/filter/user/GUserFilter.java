package lsfusion.gwt.client.form.filter.user;

// an ENTRY of a table's user filters (GUserFiltersController): a condition with a key of its own - the entry itself -,
// which keeps it across what changes the condition, so what draws it keeps its row, its editor and its marks
public final class GUserFilter {
    // its condition, the model's own, changed by the model alone
    public final GPropertyFilter condition;
    // a NULL allowed: with no value it compares with NULL; else, with none, it is inactive - kept, not sent
    public boolean allowNull;
    // a value committed in it, or it sent, or the server's: it is the user's then, and Escape no longer takes it away
    public boolean confirmed;
    // in the list sent or reported last, which is what its row shows as applied
    public boolean applied;
    // the FILTER whose place it holds while no active entry is on its column, what that remembers of its condition
    // in it (GUserFiltersController.normalize); null for every other entry
    GFilter placeholderOf;

    GUserFilter(GPropertyFilter condition, boolean allowNull) {
        this.condition = condition;
        this.allowNull = allowNull;
    }

    // whether it is sent: it has a value, or the NULL it allows
    public boolean isActive() {
        return !condition.nullValue() || allowNull;
    }
}
