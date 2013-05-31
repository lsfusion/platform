package platform.gwt.form.client.form.ui.toolbar.preferences;

import com.allen_sauer.gwt.dnd.client.DragController;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import platform.gwt.form.client.form.ui.GCaptionPanel;
import platform.gwt.form.shared.view.GPropertyDraw;

import java.util.ArrayList;

public class ColumnsDualListBox extends AbsolutePanel {
    private static final String CSS_DUAL_LIST_BUTTONS_CONTAINER = "dualListButtonsContainer";
    private static final String CSS_DUAL_LIST_CONTAINER = "dualListContainer";

    private ColumnsListBoxDragController dragController;

    private ColumnsListBox left;

    private ColumnsListBox right;

    public ColumnsDualListBox() {
        setSize("100%", "100%");

        HorizontalPanel horizontalPanel = new HorizontalPanel();
        add(horizontalPanel);
        horizontalPanel.addStyleName(CSS_DUAL_LIST_CONTAINER);
        horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

        VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.addStyleName(CSS_DUAL_LIST_BUTTONS_CONTAINER);
        verticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        dragController = new ColumnsListBoxDragController(this);
        left = new ColumnsListBox(dragController) {
            @Override
            public void doubleclicked() {
                moveItems(left, right, true);
            }
        };
        right = new ColumnsListBox(dragController) {
            @Override
            public void doubleclicked() {
                moveItems(right, left, true);
            }
        };

        ColumnsListContainer leftFocusPanel = new ColumnsListContainer(left);
        leftFocusPanel.setHeight("100%");
        GCaptionPanel leftColumns = new GCaptionPanel("Отображаемые колонки", leftFocusPanel);
        horizontalPanel.add(leftColumns);
        horizontalPanel.setCellHeight(leftColumns, "100%");
        horizontalPanel.setCellWidth(leftColumns, "43%");

        horizontalPanel.add(verticalPanel);
        horizontalPanel.setCellWidth(verticalPanel, "6em");

        ColumnsListContainer rightFocusPanel = new ColumnsListContainer(right);
        rightFocusPanel.setHeight("100%");
        GCaptionPanel rightColumns = new GCaptionPanel("Спрятанные колонки", rightFocusPanel);
        horizontalPanel.add(rightColumns);
        horizontalPanel.setCellHeight(rightColumns, "100%");

        horizontalPanel.setSize("100%", "100%");

        Button oneRight = new Button("&gt;");
        Button oneLeft = new Button("&lt;");
        Button allRight = new Button("&gt;&gt;");
        Button allLeft = new Button("&lt;&lt;");
        verticalPanel.add(oneRight);
        verticalPanel.add(oneLeft);
        verticalPanel.add(new HTML("&nbsp;"));
        verticalPanel.add(allRight);
        verticalPanel.add(allLeft);

        allRight.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveItems(left, right, false);
            }
        });

        allLeft.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveItems(right, left, false);
            }
        });

        oneRight.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveItems(left, right, true);
            }
        });

        oneLeft.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveItems(right, left, true);
            }
        });

        ColumnsListBoxDropController leftDropController = new ColumnsListBoxDropController(left);
        ColumnsListBoxDropController rightDropController = new ColumnsListBoxDropController(right);
        ColumnsListContainerDropController rightPanelDropController = new ColumnsListContainerDropController(rightFocusPanel);
        ColumnsListContainerDropController leftPanelDropController = new ColumnsListContainerDropController(leftFocusPanel);
        dragController.registerDropController(leftDropController);
        dragController.registerDropController(rightDropController);
        dragController.registerDropController(rightPanelDropController);
        dragController.registerDropController(leftPanelDropController);
    }

    public void addVisible(GPropertyDraw property) {
        left.add(property);
    }

    public void addInvisible(GPropertyDraw property) {
        right.add(property);
    }

    public DragController getDragController() {
        return dragController;
    }

    protected void moveItems(ColumnsListBox from, ColumnsListBox to, boolean justSelectedItems) {
        ArrayList<Widget> widgetList = justSelectedItems ? dragController.getSelectedWidgets(from)
                : from.widgetList();
        for (Widget widget : widgetList) {
            from.remove(widget);
            to.add(widget);
        }
    }

    public ArrayList<Widget> getVisibleWidgets() {
        return left.widgetList();
    }

    public ArrayList<Widget> getInvisibleWidgets() {
        return right.widgetList();
    }

    public void clearLists() {
        left.clear();
        right.clear();
    }
}