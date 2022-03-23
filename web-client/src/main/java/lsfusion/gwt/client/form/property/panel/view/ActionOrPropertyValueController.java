package lsfusion.gwt.client.form.property.panel.view;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

public interface ActionOrPropertyValueController {

    void setValue(GGroupObjectValue columnKey, Object value);

    void setLoading(GGroupObjectValue columnKey, Object value);
}
