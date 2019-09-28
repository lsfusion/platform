package lsfusion.gwt.client.form.event;

public class GMouseInputEvent extends GInputEvent {

    public static final GMouseInputEvent DBLCLK = new GMouseInputEvent("DBLCLK");

    public String mouseEvent;

    public GMouseInputEvent() {
    }

    public GMouseInputEvent(String mouseEvent) {
        this.mouseEvent = mouseEvent;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GMouseInputEvent && mouseEvent.equals(((GMouseInputEvent) o).mouseEvent);
    }

    @Override
    public int hashCode() {
        return mouseEvent.hashCode();
    }

}
