package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.object.table.grid.user.design.PropertyListItem;

import java.util.ArrayList;

import static lsfusion.gwt.client.view.MainFrame.v5;


public abstract class ColumnsListBox extends Composite {

    private ColumnsListBoxDragController dragController;

    private Grid grid;
    
    public boolean visible;

    public ColumnsListBox() {
        grid = new Grid();

        initWidget(grid);
        grid.resizeColumns(1);
        grid.setCellPadding(0);
        grid.setCellSpacing(0);

        grid.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                singleclicked();     
            }
        });
        grid.addDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                doubleclicked();
            }
        });
        setWidth("100%");
        GwtClientUtils.addClassName(this, "draggable-list-box", "draggableListBox", v5);
    }

    public ColumnsListBox(ColumnsListBoxDragController dragController, boolean visible) {
        this();
        this.dragController = dragController;
        this.visible = visible;
    }

    public void add(PropertyListItem property) {
        add(createLabel(property));
    }

    public void add(Widget widget) {
        if (widget instanceof PropertyLabel) {
            ((PropertyLabel) widget).refreshLabel(visible);
        }
        setWidget(getItemCount(), widget);
    }

    public void add(int position, PropertyListItem property) {
        setWidget(position, createLabel(property));
    }

    private PropertyLabel createLabel(PropertyListItem property) {
        return new PropertyLabel(property, visible);
    }

    public int getItemCount() {
        return grid.getRowCount();
    }

    public boolean remove(Widget widget)      {
        int index = getWidgetIndex(widget);
        if (index == -1) {
            return false;
        }
        grid.removeRow(index);
        return true;
    }

    public ArrayList<Widget> widgetList() {
        ArrayList<Widget> widgetList = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            widgetList.add(getWidget(i));
        }
        return widgetList;
    }

    public Widget getWidget(int index) {
        return grid.getWidget(index, 0);
    }

    private int getWidgetIndex(Widget widget) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getWidget(i) == widget) {
                return i;
            }
        }
        return -1;
    }

    protected void setWidget(int index, Widget widget) {
        grid.insertRow(index);

        GwtClientUtils.addClassName(grid.getCellFormatter().getElement(index, 0), "draggable-list-box-item", "draggableListBoxItem", v5);
        if (dragController != null) {
            dragController.makeDraggable(widget);
        }

        if (widget instanceof PropertyLabel) {
//            ((PropertyLabel) widget).getPropertyItem().setVisible(visible);
            ((PropertyLabel) widget).refreshLabel(visible);
        }
        grid.setWidget(index, 0, widget);

    }

    public void clear() {
        for (Widget w : widgetList()) {
            remove(w);
        }
    }

    public abstract void singleclicked();

    public abstract void doubleclicked();
}