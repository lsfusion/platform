package lsfusion.client.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.StringPropertyEditor;
import lsfusion.client.form.property.cell.classes.view.StringPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static lsfusion.interop.form.property.Compare.*;

public class ClientStringClass extends ClientAStringClass {

    public ClientStringClass(boolean blankPadded, boolean caseInsensitive, ExtInt length) {
        super(blankPadded, caseInsensitive, length);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new StringPropertyRenderer(property);
    }

    @Override
    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        if(length.isUnlimited())
            return super.getValueEditorComponent(form, property, asyncChange, value);
        return new StringPropertyEditor(property, asyncChange, value, length.getValue(), !blankPadded, false);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new StringPropertyEditor(property, asyncChange, value, length.isUnlimited() ? Integer.MAX_VALUE : length.getValue(), !blankPadded, true);
    }
}
