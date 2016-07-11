package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;


public abstract class ColumnsListBox extends Composite {
    private static final String CSS_DRAGGABLE_LIST_BOX_ITEM = "draggableListBoxItem";
    private static final String CSS_DRAGGABLE_LIST_BOX = "draggableListBox";

    private ColumnsListBoxDragController dragController;

    private Grid grid;

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
        addStyleName(CSS_DRAGGABLE_LIST_BOX);
    }

    public ColumnsListBox(ColumnsListBoxDragController dragController) {
        this();
        this.dragController = dragController;
    }

    public void add(PropertyListItem property) {
        add(createLabel(property));
    }

    public void add(Widget widget) {
        setWidget(getItemCount(), widget);
    }

    public void add(int position, PropertyListItem property) {
        setWidget(position, createLabel(property));
    }

    private PropertyLabel createLabel(PropertyListItem property) {
        PropertyLabel label = new PropertyLabel(property);
        return label;
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

        grid.getCellFormatter().addStyleName(index, 0, CSS_DRAGGABLE_LIST_BOX_ITEM);
        if (dragController != null) {
            dragController.makeDraggable(widget);
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