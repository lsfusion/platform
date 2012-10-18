package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Element;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.changes.dto.ColorDTO;
import platform.gwt.form2.shared.view.grid.GridEditableCell;

import java.util.Arrays;

public class GSinglePropertyTable extends GPropertyTable {
    private String background;
    private String foreground;

    /**
     * Default style's overrides
     */
    public interface GSinglePropertyTableResource extends Resources {
        @Source("GSinglePropertyTable.css")
        GSinglePropertyTableStyle dataGridStyle();
    }
    public interface GSinglePropertyTableStyle extends Style {}

    public static final GSinglePropertyTableResource GSINGLE_PROPERTY_TABLE_RESOURCE = GWT.create(GSinglePropertyTableResource.class);

    private final GPropertyDraw property;
    private GGroupObjectValue columnKey;
    private Object value;

    public GSinglePropertyTable(GFormController iform, GPropertyDraw iproperty, GGroupObjectValue columnKey) {
        super(iform, GSINGLE_PROPERTY_TABLE_RESOURCE);

        this.property = iproperty;
        this.columnKey = columnKey;

        setTableBuilder(new GSinglePropertyTableCellBuilder(this));

        addColumn(new Column<Object, Object>(new GridEditableCell(this)) {
            @Override
            public Object getValue(Object record) {
                return value;
            }
        });
        setRowData(Arrays.asList(new Object()));

        //нижеследующее нужно только для работы в zoom-режиме, т.к. из-за погрешностей скроллбары выезжают на область таблицы
        getScrollPanel().removeHorizontalScrollbar();
        getScrollPanel().removeVerticalScrollbar();
        //хак, но сейчас api не предоставляет публичного доступа к ScrollPanel.scrollableElement, который нам нужен, поэтому делаем, жёстко полагаясь на реализацию
        Element.as(getScrollPanel().getElement().getChild(1)).
                getFirstChildElement().getStyle().setOverflow(Overflow.HIDDEN);
    }

    // перегруженный метод из AbstractCellTable
    protected boolean isSingleCellTable() {
        return true;
    }

    public void setValue(Object value) {
        this.value = value;
        redraw();
    }

    public void setBackground(ColorDTO background) {
        this.background = background == null ? null : background.toString();
        redraw();
    }

    public void setForeground(ColorDTO foreground) {
        this.foreground = foreground == null ? null : foreground.toString();
        redraw();
    }

    public String getBackground() {
        return background;
    }

    public String getForeground() {
        return foreground;
    }

    public GPropertyDraw getProperty(int row, int column) {
        assert row == 0 && column == 0;
        return property;
    }

    @Override
    public GGroupObjectValue getColumnKey(int row, int column) {
        return columnKey;
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
}
