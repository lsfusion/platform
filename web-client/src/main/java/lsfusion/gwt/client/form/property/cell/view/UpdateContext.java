package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.PValue;

public interface UpdateContext {
    
    default void getAsyncValues(String value, String actionSID, AsyncCallback<GFormController.GAsyncResult> callback) {}
    default void changeProperty(PValue result, GFormController.ChangedRenderValueSupplier renderValueSupplier) {}
    default void executeContextAction(int action) {}

    default Boolean isPropertyReadOnly() { return false; }

    default boolean isTabFocusable() { return false; }

    boolean globalCaptionIsDrawn();

    PValue getValue();

    default boolean isLoading() { return false; }

    default AppBaseImage getImage() { return null; }

    boolean isSelectedRow();
    default boolean isSelectedLink() { return isSelectedRow(); }

    default CellRenderer.ToolbarAction[] getToolbarActions() { return CellRenderer.noToolbarActions; } ;

    default String getBackground() { return null; }

    default String getForeground() { return null; }

    default String getPlaceholder() { return null; }

    default String getTooltip() { return null; }

    default String getValueTooltip() { return null; }

    default String getValueAttr() { return null; }

    default String getValueElementClass() {
        return null;
    }

    RendererType getRendererType();
}
