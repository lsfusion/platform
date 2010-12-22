package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.CellView;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;

import java.awt.*;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

public interface ClientType {

    int getMinimumWidth(FontMetrics fontMetrics);

    int getPreferredWidth(FontMetrics fontMetrics);

    int getMaximumWidth(FontMetrics fontMetrics);

    int getPreferredHeight(FontMetrics fontMetrics);

    Format getDefaultFormat();

    PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design);

    CellView getPanelComponent(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form);

    PropertyEditorComponent getEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException;

    PropertyEditorComponent getObjectEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException;

    PropertyEditorComponent getClassComponent(ClientFormController form, ClientPropertyDraw property, Object value, Format format) throws IOException, ClassNotFoundException;

    Object parseString(String s) throws ParseException;

    boolean shouldBeDrawn(ClientFormController form);
    
    String getConformedMessage();

    ClientTypeClass getTypeClass();
}
