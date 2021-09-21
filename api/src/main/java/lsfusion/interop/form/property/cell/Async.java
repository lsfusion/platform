package lsfusion.interop.form.property.cell;

import java.io.Serializable;

public class Async implements Serializable {
    public final String displayString;
    public final String rawString;

    public static final Async RECHECK = new Async("RECHECK", "RECHECK");
    public static final Async CANCELED = new Async("CANCELED", "CANCELED");

    public Async(String displayString, String rawString) {
        this.displayString = displayString;
        this.rawString = rawString;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Async && displayString.equals(((Async) o).displayString) && rawString.equals(((Async) o).rawString);
    }

    @Override
    public int hashCode() {
        return displayString.hashCode() * 31 + rawString.hashCode();
    }

    @Override
    public String toString() {
        return "<html>" + displayString + "</html>";
    }
}
