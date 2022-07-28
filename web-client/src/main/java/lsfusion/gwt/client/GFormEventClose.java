package lsfusion.gwt.client;

import java.util.Objects;

public class GFormEventClose extends GFormEvent {
    public boolean ok;

    @SuppressWarnings("unused")
    public GFormEventClose() {
        super();
    }

    public GFormEventClose(boolean ok) {
        super();
        this.ok = ok;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GFormEventClose that = (GFormEventClose) o;
        return ok == that.ok;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ok);
    }
}