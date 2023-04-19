package lsfusion.gwt.client.base.view.popup;

import lsfusion.gwt.client.form.property.PValue;

public interface PopupMenuItemValue {
    String getDisplayString();

    String getReplacementString();

    default PValue getPValue() {
        throw new UnsupportedOperationException();
    }

    default String getTooltipString() {
        return getDisplayString();
    }
}
