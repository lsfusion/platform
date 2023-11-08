package lsfusion.gwt.client.form.property.cell.view;

public enum RendererType {
    CELL, VALUE;

    public static final RendererType GRID = CELL;
    public static final RendererType PANEL = CELL;
    public static final RendererType SIMPLE = CELL; // ??? maybe for MAP / CALENDAR, should be VALUE

    public static final RendererType PIVOT = VALUE;
    public static final RendererType FILTER = VALUE;
    public static final RendererType FOOTER = VALUE;

}
