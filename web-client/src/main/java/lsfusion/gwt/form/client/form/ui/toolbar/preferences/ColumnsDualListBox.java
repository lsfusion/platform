package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.allen_sauer.gwt.dnd.client.DragController;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.client.form.ui.GCaptionPanel;

import java.util.ArrayList;

public class ColumnsDualListBox extends AbsolutePanel {
    private static final String CSS_DUAL_LIST_BUTTONS_CONTAINER = "dualListButtonsContainer";
    private static final String CSS_DUAL_LIST_CONTAINER = "dualListContainer";

    private ColumnsListBoxDragController dragController;

    private ColumnsListBox left;

    private ColumnsListBox right;
    
    private TextBox columnCaptionBox;

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
            public void singleclicked() {
                setColumnCaptionBoxText(left, true);
            }

            @Override
            public void doubleclicked() {
                moveItems(left, right, true);
            }
        };
        
        right = new ColumnsListBox(dragController) {
            @Override
            public void singleclicked() {
                setColumnCaptionBoxText(right, true);
            }

            @Override
            public void doubleclicked() {
                moveItems(right, left, true);
            }
        };

        ColumnsListContainer leftFocusPanel = new ColumnsListContainer(left);
        leftFocusPanel.setHeight("100%");
        GCaptionPanel leftColumns = new GCaptionPanel("Отображаемые колонки", leftFocusPanel);
        leftColumns.setSize("100%", "100%");
        horizontalPanel.add(leftColumns);
        horizontalPanel.setCellHeight(leftColumns, "100%");
        horizontalPanel.setCellWidth(leftColumns, "43%");

        horizontalPanel.add(verticalPanel);
        horizontalPanel.setCellWidth(verticalPanel, "6em");

        ColumnsListContainer rightFocusPanel = new ColumnsListContainer(right);
        rightFocusPanel.setHeight("100%");
        GCaptionPanel rightColumns = new GCaptionPanel("Спрятанные колонки", rightFocusPanel);
        rightColumns.setSize("100%", "100%");
        horizontalPanel.add(rightColumns);
        horizontalPanel.setCellHeight(rightColumns, "100%");

        horizontalPanel.setSize("100%", "80%");

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
       
        // column caption settings        
        columnCaptionBox = new TextBox();
        columnCaptionBox.setSize("100%", "100%");
        columnCaptionBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                if(getSelectedWidget(left) != null) {
                    String columnCaption = columnCaptionBox.getText();
                    if(!columnCaption.isEmpty()) {
                        getSelectedWidget(left).setText(columnCaption);
                        getSelectedWidget(left).setUserCaption(columnCaption);
                    }
                }
                else if(getSelectedWidget(right) != null) {
                    String columnCaption = columnCaptionBox.getText();
                    if(!columnCaption.isEmpty()) {
                        getSelectedWidget(right).setText(columnCaption);
                        getSelectedWidget(right).setUserCaption(columnCaption );
                    }
                }
            }
        });

        FlexPanel columnCaptionPanel = new FlexPanel();
        columnCaptionPanel.add(new Label("Заголовок колонки " + ": "), GFlexAlignment.CENTER);
        columnCaptionPanel.add(columnCaptionBox, GFlexAlignment.CENTER);

        GCaptionPanel columnCaptionSettingsPanel = new GCaptionPanel("Настройки выбранной колонки", columnCaptionPanel);
        add(columnCaptionSettingsPanel);
        add(GwtClientUtils.createVerticalStrut(5));
    }

    public void addVisible(PropertyListItem property) {
        left.add(property);
    }

    public void addInvisible(PropertyListItem property) {
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

    protected PropertyLabel getSelectedWidget(ColumnsListBox list) {
        ArrayList<Widget> selectedWidgets = dragController.getSelectedWidgets(list);
        return selectedWidgets.size() == 0 ? null : (PropertyLabel)(selectedWidgets.get(selectedWidgets.size() - 1));
    }
    
    protected void setColumnCaptionBoxText(ColumnsListBox list, boolean ignoreDefault) {
        PropertyLabel selectedWidget = getSelectedWidget(list);
        if(selectedWidget != null)
            columnCaptionBox.setText(selectedWidget.getUserCaption(ignoreDefault));
    }

    public ArrayList<Widget> getVisibleWidgets() {
        return left.widgetList();
    }

    public ArrayList<Widget> getInvisibleWidgets() {
        return right.widgetList();
    }
    
    public int getVisibleCount() {
        return left.getItemCount();
    }
    
    public int getVisibleIndex(Widget w) {
        return left.widgetList().indexOf(w);
    }

    public int getInvisibleIndex(Widget w) {
        return right.widgetList().indexOf(w);
    }

    public void clearLists() {
        left.clear();
        right.clear();
    }
}