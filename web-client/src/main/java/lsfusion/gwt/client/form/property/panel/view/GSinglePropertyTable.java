package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.SinglePropertyTableStyle;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.view.GridEditableCell;
import lsfusion.gwt.client.form.property.table.view.GPropertyTable;

import java.util.Arrays;
import java.util.List;

import static com.google.gwt.dom.client.Style.Unit;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class GSinglePropertyTable extends GPropertyTable<Object> {
    public static final SinglePropertyTableStyle SINGLE_PROPERTY_TABLE_STYLE = new SinglePropertyTableStyle();

    private final GPropertyDraw property;
    private GGroupObjectValue columnKey;
    private Object value;
    private boolean readOnly = false;
    private boolean autoSizedHeight = false; //пока не вижу возможности обновлять рекурсивно все компоненты до верха при изменении высоты элемента

    private String background;
    private String foreground;

    public GSinglePropertyTable(GFormController iform, GPropertyDraw iproperty, GGroupObjectValue columnKey) {
        super(iform, null, SINGLE_PROPERTY_TABLE_STYLE, true, true, true);

        this.property = iproperty;
        this.columnKey = columnKey;

        setTableBuilder(new GSinglePropertyTableBuilder(this));

        setCellHeight(property.getValueHeight(null));
        setRemoveKeyboardStylesOnBlur(true);

        addColumn(new Column<Object, Object>(new GridEditableCell(this)) {
            @Override
            public Object getValue(Object record) {
                return value;
            }
        });
        setRowData(Arrays.asList(new Object()));
    }

    public void setupFillParent() {
        getTableElement().getStyle().setHeight(100, Unit.PCT); // not sure what for
        ((GSinglePropertyTableBuilder)getTableBuilder()).setStripCellHeight(true);
    }

    public void setValue(Object value) {
        if (!GwtSharedUtils.nullEquals(this.value, value)) {
            this.value = value;
            redraw();
        }
        if(!autoSizedHeight && property.autoSize && value instanceof String) {
            int width = getElement().getClientWidth();
            if(width > 0) {
                int height = getHeight((String) value, width);
                getParent().setHeight(height + "px");
                autoSizedHeight = true;
                redraw();
            }
        }
    }

    private int getHeight(String text, int maxWidth) {
        int rows = 0;
        if (text != null) {
            String[] lines = text.split("\n");
            rows += lines.length;
            for(String line : lines) {
                String[] splittedText = line.split(" ");
                String output = "";
                int outputWidth = 0;
                int spaceWidth = GFontMetrics.getSymbolWidth(null);
                int wordWidth;
                int j = 1;

                for (String word : splittedText) {
                    wordWidth = 0;
                    for (int i = 0; i < word.length(); i++)
                        wordWidth += GFontMetrics.getSymbolWidth(null);
                    if ((outputWidth + spaceWidth + wordWidth) < maxWidth) {
                        output = output.concat(" ").concat(word);
                        outputWidth += spaceWidth + wordWidth;
                    } else {
                        rows++;
                        output = word;
                        outputWidth = wordWidth;
                        j = j + 1;
                    }
                }
            }
        }
        return (rows + 1) * GFontMetrics.getSymbolHeight(null);
    }

    public void setReadOnly(boolean readOnly) {
        if (this.readOnly != readOnly) {
            this.readOnly = readOnly;
            redraw();
        }
    }

    public void setBackground(ColorDTO background) {
        String sBackground = background == null ? null : background.toString();
        if (!GwtSharedUtils.nullEquals(this.background, sBackground)) {
            this.background = sBackground;
            redraw();
        }
    }

    public void setForeground(ColorDTO foreground) {
        String sForeground = foreground == null ? null : foreground.toString();
        if (!GwtSharedUtils.nullEquals(this.foreground, sForeground)) {
            this.foreground = sForeground;
            redraw();
        }
    }

    public String getBackground() {
        return background;
    }

    public String getForeground() {
        return foreground;
    }

    @Override
    public GPropertyDraw getSelectedProperty() {
        return property;
    }

    @Override
    public GGroupObjectValue getSelectedColumn() {
        return columnKey;
    }

    @Override
    protected boolean drawFocusedCellBorder() {
        return false;
    }

    public GPropertyDraw getProperty(Cell.Context context) {
        assert context.getIndex() == 0 && context.getColumn() == 0;
        return property;
    }

    @Override
    public GGroupObjectValue getColumnKey(Cell.Context context) {
        return columnKey;
    }

    @Override
    public boolean isEditable(Cell.Context context) {
        return !readOnly;
    }

    @Override
    public void setValueAt(Cell.Context context, Object value) {
        assert context.getIndex() == 0 && context.getColumn() == 0;
        setValue(value);
    }

    @Override
    public Object getValueAt(Cell.Context context) {
        assert context.getIndex() == 0 && context.getColumn() == 0;
        return value;
    }

    @Override
    public void pasteData(List<List<String>> table) {
        if (!table.isEmpty() && !table.get(0).isEmpty()) {
            form.pasteSingleValue(property, columnKey, table.get(0).get(0));
        }
    }

    @Override
    public void selectNextRow(boolean down) {
    }

    @Override
    public void selectNextCellInColumn(boolean forward) {
    }

    @Override
    protected void onBrowserEvent2(Event event) {
        GGridController groupController = form.getController(property.groupObject);
        if (groupController != null) {
            if (GKeyStroke.isRemoveAllFiltersEvent(event)) {
                stopPropagation(event);
                groupController.removeFilters();
            } else if (GKeyStroke.isReplaceFilterEvent(event)) {
                stopPropagation(event);
                groupController.replaceFilter();
            } else {
                super.onBrowserEvent2(event);
            }
        } else {
            super.onBrowserEvent2(event);
        }
    }
}
