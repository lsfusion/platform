package lsfusion.gwt.form.shared.view.grid;

public class InternalEditEvent extends EditEvent {
    private final String action;

    public InternalEditEvent(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    @Override
    public void stopPropagation() {
        //do nothing
    }
}
