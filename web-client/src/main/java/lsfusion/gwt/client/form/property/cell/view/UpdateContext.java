package lsfusion.gwt.client.form.property.cell.view;

import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.form.property.PValue;

public interface UpdateContext {
    
    default void changeProperty(PValue result) {}
    default void executeContextAction(int action) {}

    default boolean isPropertyReadOnly() { return true; }

    boolean globalCaptionIsDrawn();

    PValue getValue();

    default boolean isLoading() { return false; }

    default AppBaseImage getImage() { return null; }

    boolean isSelectedRow();
    default boolean isSelectedLink() { return isSelectedRow(); }

    default CellRenderer.ToolbarAction[] getToolbarActions() { return CellRenderer.noToolbarActions; } ;

    default String getBackground() { return null; }

    default String getForeground() { return null; }

    default String getValueElementClass() {
        return null;
    }
}
