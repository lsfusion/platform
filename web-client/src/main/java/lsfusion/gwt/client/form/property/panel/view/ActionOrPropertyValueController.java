package lsfusion.gwt.client.form.property.panel.view;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.PValue;

public interface ActionOrPropertyValueController {

    void setValue(GGroupObjectValue columnKey, PValue value);

    void setLoading(GGroupObjectValue columnKey, PValue value);

    void startEditing(GGroupObjectValue columnKey);
    void stopEditing(GGroupObjectValue columnKey);

    String getCaption(GGroupObjectValue columnKey);
}
