package lsfusion.gwt.client.form.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ui.GKeyStroke;
import lsfusion.gwt.shared.base.GwtSharedUtils;
import lsfusion.gwt.client.cellview.Column;
import lsfusion.gwt.client.cellview.cell.Cell;
import lsfusion.gwt.shared.form.view.GFontMetrics;
import lsfusion.gwt.shared.form.view.GPropertyDraw;
import lsfusion.gwt.shared.form.changes.GGroupObjectValue;
import lsfusion.gwt.shared.form.view.dto.ColorDTO;
import lsfusion.gwt.client.form.grid.GridEditableCell;

import java.util.Arrays;
import java.util.List;

import static com.google.gwt.dom.client.Style.Unit;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class GSinglePropertyTable extends GPropertyTable<Object> {
    /**
     * Default style's overrides
     */
    public interface GSinglePropertyTableResource extends Resources {
        @Source("GSinglePropertyTable.css")
        GSinglePropertyTableStyle style();
    }
    public interface GSinglePropertyTableStyle extends Style {
        String dataGridCellInnerDiv();
    }

    public static final GSinglePropertyTableResource GSINGLE_PROPERTY_TABLE_RESOURCE = GWT.create(GSinglePropertyTableResource.class);

    private final GPropertyDraw property;
    private GGroupObjectValue columnKey;
    private Object value;
    private boolean readOnly = false;
    private boolean autoSizedHeight = false; //пока не вижу возможности обновлять рекурсивно все компоненты до верха при изменении высоты элемента

    private String background;
    private String foreground;

    public GSinglePropertyTable(GFormController iform, GPropertyDraw iproperty, GGroupObjectValue columnKey) {
        super(iform, GSINGLE_PROPERTY_TABLE_RESOURCE, true);

        this.property = iproperty;
        this.columnKey = columnKey;

        setTableBuilder(new GSinglePropertyTableBuilder(this));

        setCellHeight(property.getValueHeight(null));
        setRemoveKeyboardStylesOnBlur(true);

        getTableDataScroller().removeScrollbars();

        addColumn(new Column<Object, Object>(new GridEditableCell(this)) {
            @Override
            public Object getValue(Object record) {
                return value;
            }
        });
        setRowData(Arrays.asList(new Object()));
    }

    public void setupFillParent() {
        GwtClientUtils.setupFillParent(getElement());
        GwtClientUtils.setupFillParent(getTableDataScroller().getContainerElement());
        getTableElement().getStyle().setHeight(100, Unit.PCT);
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
    public void selectNextCellInColumn(boolean down) {
    }

    @Override
    protected void onBrowserEvent2(Event event) {
        GGroupObjectController groupController = form.getController(property.groupObject);
        if (groupController != null) {
            if (GKeyStroke.isAddFilterEvent(event)) {
                stopPropagation(event);
                groupController.addFilter();
            } else if (GKeyStroke.isRemoveAllFiltersEvent(event)) {
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
