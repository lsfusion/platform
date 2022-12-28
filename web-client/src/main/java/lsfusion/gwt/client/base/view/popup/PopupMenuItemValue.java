package lsfusion.gwt.client.base.view.popup;

public interface PopupMenuItemValue {
    String getDisplayString();

    String getReplacementString();

    default String getTooltipString() {
        return getDisplayString();
    }
}
