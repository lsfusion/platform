package lsfusion.client.logics.classes;

import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.cell.PanelView;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Compare;

import java.awt.*;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

public interface ClientType {

    int getMinimumWidth(int minCharWidth, FontMetrics fontMetrics);

    int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics);

    int getMaximumWidth(int maxCharWidth, FontMetrics fontMetrics);

    int getPreferredHeight(FontMetrics fontMetrics);

    int getMaximumHeight(FontMetrics fontMetrics);

    Format getDefaultFormat();

    PropertyRenderer getRendererComponent(ClientPropertyDraw property);

    PanelView getPanelView(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form);

    PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value);

    PropertyEditor getObjectEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException;

    PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value);

    Object parseString(String s) throws ParseException;

    String formatString(Object obj) throws ParseException;

    Object transformServerValue(Object obj);

    boolean shouldBeDrawn(ClientFormController form);
    
    String getConfirmMessage();

    ClientTypeClass getTypeClass();

    Compare[] getFilterCompares();

    Compare getDefaultCompare();
}
