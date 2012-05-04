package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.CellView;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.gwt.view.classes.GType;
import platform.interop.Compare;
import platform.interop.ComponentDesign;

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

    PropertyRendererComponent getRendererComponent(String caption, ClientPropertyDraw property);

    CellView getPanelComponent(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form);

    PropertyEditorComponent getEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException;

    PropertyEditorComponent getObjectEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException;

    PropertyEditorComponent getClassComponent(ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException;

    Object parseString(String s) throws ParseException;

    String formatString(Object obj) throws ParseException;

    boolean shouldBeDrawn(ClientFormController form);
    
    String getConformedMessage();

    ClientTypeClass getTypeClass();

    Compare[] getFilerCompares();

    Compare getDefaultCompare();

    GType getGwtType();
}
