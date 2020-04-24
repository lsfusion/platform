package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.AbstractCell;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.NativeEditEvent;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;
import lsfusion.gwt.client.form.property.panel.view.GSinglePropertyTable;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.text.ParseException;
import java.util.Arrays;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static com.google.gwt.dom.client.Style.Unit;
import static lsfusion.gwt.client.base.GwtClientUtils.removeAllChildren;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class GDataFilterValueViewTable extends DataGrid implements EditManager {
    private GDataFilterValueView valueView;
    private GPropertyDraw property;

    private Object value;

    private DataFilterValueEditableCell cell;

    public GDataFilterValueViewTable(GDataFilterValueView valueView, GPropertyDraw property) {
        super(GSinglePropertyTable.SINGLE_PROPERTY_TABLE_STYLE);

        this.valueView = valueView;
        this.property = property;

        sinkEvents(Event.ONPASTE);

        setRemoveKeyboardStylesOnBlur(true);

        setSize("100%", property.getValueHeight(null) + "px");
        setTableWidth(property.getValueWidth(null) + 2, Unit.PX);  // 2 for borders
        getTableDataScroller().removeScrollbars();

        cell = new DataFilterValueEditableCell();

        addColumn(new Column<Object, Object>(cell) {
            @Override
            public Object getValue(Object record) {
                return value;
            }
        });
        setTableBuilder(new GPropertyTableBuilder<Object>(this) {
            @Override
            public String getBackground(Object rowValue, int row, int column) {
                return null;
            }

            @Override
            public String getForeground(Object rowValue, int row, int column) {
                return null;
            }
        });
        setRowData(Arrays.asList(new Object()));
    }

    public void setProperty(GPropertyDraw property) {
        this.property = property;

        int pixelHeight = property.getValueHeight(null);

        setTableWidth(property.getValueWidth(null) + 2, Unit.PX); // 2 for borders
        setHeight(pixelHeight + "px");

        setCellHeight(pixelHeight);
    }

    private void setCellHeight(int cellHeight) {
        ((GPropertyTableBuilder) getTableBuilder()).setCellHeight(cellHeight);
        setRowHeight(cellHeight + 1); //1px for border
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
        cell.update();
    }

    @Override
    protected boolean drawFocusedCellBorder() {
        return false;
    }

    public void focusOnValue() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                setFocus(true);

                Element focusElement = getChildElement(0).getCells().getItem(0).getFirstChildElement();
                CopyPasteUtils.setEmptySelection(focusElement);
            }
        });
    }

    @Override
    public void commitEditing(Object value) {
        cell.finishEditing();
        valueView.valueChanged(value);
    }

    @Override
    public void cancelEditing() {
        cell.finishEditing();
    }

    @Override
    public void selectNextRow(boolean down) {
    }

    @Override
    public void selectNextCellInColumn(boolean forward) {
    }

    public void startEditing(EditEvent event) {
        cell.beginEditing(event);
    }

    @Override
    protected void onBrowserEvent2(Event event) {
        if (cell.cellEditor == null && event.getTypeInt() == Event.ONPASTE) { // пока работает только для Chrome
            executePaste(event);
        } else {
            super.onBrowserEvent2(event);
        }
    }

    private void executePaste(Event event) {
        String line = CopyPasteUtils.getClipboardData(event).trim();
        if (!line.isEmpty()) {
            stopPropagation(event);
            line = line.replaceAll("\r\n", "\n");    // браузеры заменяют разделители строк на "\r\n"
            cell.pasteValue(line);
        }
    }

    class DataFilterValueEditableCell extends AbstractCell<Object> {
        private boolean isInEditingState = false;
        private GridCellEditor cellEditor;
        private DivElement parentElement;
        private Context context;

        public DataFilterValueEditableCell() {
            super(CLICK, KEYDOWN, KEYPRESS, BLUR);
        }

        @Override
        public boolean isEditing(Context context, Element parent, Object value) {
            return isInEditingState;
        }

        @Override
        public void renderDom(Context context, DivElement cellElement, Object value) {
            assert !isInEditingState;
            if (parentElement == null) {
                parentElement = cellElement;
            }
            if (this.context == null) {
                this.context = context;
            }

            GridCellRenderer cellRenderer = property.getGridCellRenderer();
            cellRenderer.render(cellElement, null, value, false);
        }

        @Override
        public void updateDom(Context context, DivElement cellElement, Object value) {
            assert !isInEditingState;

            GridCellRenderer cellRenderer = property.getGridCellRenderer();
            cellRenderer.renderDynamic(cellElement, null, value,false);
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, Object value, NativeEvent event) {
            if ((BrowserEvents.CLICK.equals(event.getType()) || GKeyStroke.isCommonEditKeyEvent(event) &&
                    !event.getCtrlKey() && !event.getAltKey() && !event.getMetaKey()) &&
                    cellEditor == null &&
                    event.getKeyCode() != KeyCodes.KEY_ESCAPE &&
                    event.getKeyCode() != KeyCodes.KEY_ENTER) {
                startEditing(new NativeEditEvent(event), context, parent);
                stopPropagation(event);
            }
            if (isInEditingState) {
                cellEditor.onBrowserEvent(context, parent, value, event);
            }
            if (GKeyStroke.isApplyFilterEvent(event)) {
                valueView.applyFilter();
            }
            if (GKeyStroke.isCopyToClipboardEvent(event)) {
                CopyPasteUtils.putIntoClipboard(parent);
            } else if (GKeyStroke.isPasteFromClipboardEvent(event)) {  // для IE, в котором не удалось словить ONPASTE, но он и так даёт доступ к буферу обмена
                executePaste((Event) event);
            }
        }

        public void pasteValue(final String value) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    Object objValue = null;
                    try {
                        objValue = property.baseType.parseString(value, property.pattern);
                    } catch (ParseException ignored) {}
                    renderDom(context, parentElement, objValue);
                    valueView.valueChanged(objValue);
                }
            });
        }

        public void beginEditing(final EditEvent event) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    startEditing(event, null, parentElement);
                }
            });
        }

        public void startEditing(EditEvent event, Context context, Element parent) {
            isInEditingState = true;
            cellEditor = property.createValueCellEdtor(GDataFilterValueViewTable.this);

            removeAllChildren(parent);

            cellEditor.renderDom(context, GDataFilterValueViewTable.this, parent.<DivElement>cast(), value);
            cellEditor.startEditing(event, context, parent == null ? getElement().getParentElement() : parent, value);
        }

        public void finishEditing() {
            isInEditingState = false;
            cellEditor = null;
            update();
        }

        public void update() {
            if (context != null && parentElement != null) {
                removeAllChildren(parentElement);
                renderDom(context, parentElement, value);

                final Runnable redrawAndFocus = new Runnable() {
                    public void run() {
                        redraw();
                        focusOnValue();
                    }
                };
                // hack, the problem is in DataGrid.resolvePendingStateAfterUpdate setFocus is called in isResolvingState, which leads to validateAndCommit => finishEditing => nested update, so in that case just schedule redraw and focus later
                if(!GDataFilterValueViewTable.this.isResolvingState)
                    redrawAndFocus.run();
                else
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        public void execute() {
                            redrawAndFocus.run();
                        }
                    });
                }
        }
    }
}
