package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.PValue;

public interface UpdateContext {
    
    default void getAsyncValues(String value, String actionSID, AsyncCallback<GFormController.GAsyncResult> callback, int increaseValuesNeededCount) {}
    default void changeProperty(PValue result, GFormController.ChangedRenderValueSupplier renderValueSupplier) {}

    GFormController getForm();
    default boolean previewEvent(Element element, Event event) { return getForm().previewEvent(event, element); }

    default Boolean isPropertyReadOnly() { return false; }

    default boolean isTabFocusable() { return false; }

    default boolean isNavigateInput() { return false; }

    boolean globalCaptionIsDrawn();

    PValue getValue();

    default boolean isLoading() { return false; }

    default AppBaseImage getImage() { return null; }

    boolean isSelectedRow();
    default boolean isSelectedLink() { return isSelectedRow(); }

    default CellRenderer.ToolbarAction[] getToolbarActions() { return CellRenderer.noToolbarActions; } ;

    default GFont getFont() { return null; }

    default String getBackground() { return null; }

    default String getForeground() { return null; }

    default String getPlaceholder() { return null; }

    default String getPattern() { return null; }

    default String getRegexp() { return null; }

    default String getRegexpMessage() { return null; }

    default String getTooltip() { return null; }

    default String getValueTooltip() { return null; }

    default String getValueElementClass() {
        return null;
    }

    RendererType getRendererType();

    Widget getPopupOwnerWidget();
}
