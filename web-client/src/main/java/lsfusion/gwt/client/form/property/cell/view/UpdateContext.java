package lsfusion.gwt.client.form.property.cell.view;

import lsfusion.gwt.client.form.design.GFont;

public interface UpdateContext {

    GFont getFont();

    UpdateContext DEFAULT = () -> null;
}
