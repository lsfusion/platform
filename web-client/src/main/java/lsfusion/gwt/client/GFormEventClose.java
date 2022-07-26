package lsfusion.gwt.client;

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
}