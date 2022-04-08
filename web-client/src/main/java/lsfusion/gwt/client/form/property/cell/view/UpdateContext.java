package lsfusion.gwt.client.form.property.cell.view;

public interface UpdateContext {
    
    default void changeProperty(Object result) {}
    default void executeContextAction(int action) {}

    default boolean isPropertyReadOnly() { return true; }

    boolean globalCaptionIsDrawn();

    Object getValue();

    default boolean isLoading() { return false; }

    default Object getImage() { return null; }

    boolean isSelectedRow();
    default boolean isSelectedLink() { return isSelectedRow(); }

    default CellRenderer.ToolbarAction[] getToolbarActions() { return CellRenderer.noToolbarActions; } ;

    default String getBackground() { return null; }

    default String getForeground() { return null; }
}
