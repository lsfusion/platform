package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.allen_sauer.gwt.dnd.client.DragController;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.client.form.ui.GCaptionPanel;

import java.util.ArrayList;

public abstract class ColumnsDualListBox extends AbsolutePanel {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();
    private static final String CSS_DUAL_LIST_BUTTONS_CONTAINER = "dualListButtonsContainer";

    private ColumnsListBoxDragController dragController;

    private ColumnsListBox visibleList;

    private ColumnsListBox invisibleList;
    
    public ColumnsDualListBox() {
        setSize("100%", "100%");

        dragController = new ColumnsListBoxDragController(this);
        visibleList = new ColumnsListBox(dragController, true) {
            @Override
            public void singleclicked() {
                setColumnCaptionBoxText(visibleList);
                setColumnPatternBoxText(visibleList);
            }

            @Override
            public void doubleclicked() {
                moveItems(visibleList, invisibleList, true);
            }
        };
        ColumnsListContainer leftFocusPanel = new ColumnsListContainer(visibleList);
        
        invisibleList = new ColumnsListBox(dragController, false) {
            @Override
            public void singleclicked() {
                setColumnCaptionBoxText(invisibleList);
                setColumnPatternBoxText(invisibleList);
            }

            @Override
            public void doubleclicked() {
                moveItems(invisibleList, visibleList, true);
            }
        };
        ColumnsListContainer rightFocusPanel = new ColumnsListContainer(invisibleList);

        Button oneRight = new Button("&gt;");
        Button oneLeft = new Button("&lt;");
        Button allRight = new Button("&gt;&gt;");
        Button allLeft = new Button("&lt;&lt;");

        VerticalPanel buttonsPanel = new VerticalPanel();
        buttonsPanel.addStyleName(CSS_DUAL_LIST_BUTTONS_CONTAINER);
        buttonsPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        buttonsPanel.add(oneRight);
        buttonsPanel.add(oneLeft);
        buttonsPanel.add(new HTML("&nbsp;"));
        buttonsPanel.add(allRight);
        buttonsPanel.add(allLeft);
        
        HorizontalPanel buttonsPanelAligner = new HorizontalPanel();
        buttonsPanelAligner.setHeight("100%");
        buttonsPanelAligner.add(buttonsPanel);
        buttonsPanelAligner.setCellVerticalAlignment(buttonsPanel, HasVerticalAlignment.ALIGN_MIDDLE);

        DockLayoutPanel buttonsAndRightPanel = new DockLayoutPanel(Style.Unit.EM);
        buttonsAndRightPanel.addWest(buttonsPanelAligner, 5);
        buttonsAndRightPanel.add(new GCaptionPanel(messages.formGridPreferencesHiddenColumns(), rightFocusPanel));

        DockLayoutPanel dockContainer = new DockLayoutPanel(Style.Unit.PCT);
        dockContainer.setSize("100%", "100%");
        dockContainer.addWest(new GCaptionPanel(messages.formGridPreferencesDisplayedColumns(), leftFocusPanel), 43);
        dockContainer.add(buttonsAndRightPanel);

        add(dockContainer);
        

        allRight.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveItems(visibleList, invisibleList, false);
            }
        });

        allLeft.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveItems(invisibleList, visibleList, false);
            }
        });

        oneRight.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveItems(visibleList, invisibleList, true);
            }
        });

        oneLeft.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveItems(invisibleList, visibleList, true);
            }
        });

        dragController.registerDropController(new ColumnsListBoxDropController(visibleList));
        dragController.registerDropController(new ColumnsListBoxDropController(invisibleList));
        dragController.registerDropController(new ColumnsListContainerDropController(rightFocusPanel));
        dragController.registerDropController(new ColumnsListContainerDropController(leftFocusPanel));
    }

    public void addVisible(PropertyListItem property) {
        visibleList.add(property);
    }

    public void addInvisible(PropertyListItem property) {
        invisibleList.add(property);
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
    
    protected void setColumnCaptionBoxText(ColumnsListBox list) {
        PropertyLabel selectedWidget = getSelectedWidget(list);
        if(selectedWidget != null)
            setColumnCaptionBoxText(selectedWidget.getUserCaption());
    }

    protected void setColumnPatternBoxText(ColumnsListBox list) {
        PropertyLabel selectedWidget = getSelectedWidget(list);
        if(selectedWidget != null)
            setColumnPatternBoxText(selectedWidget.getUserPattern());
    }
    
    public abstract void setColumnCaptionBoxText(String text);

    public abstract void setColumnPatternBoxText(String text);

    public void columnCaptionBoxTextChanged(String columnCaption) {
        PropertyLabel propertyLabel = getSelectedWidget(visibleList);
        if (propertyLabel == null) {
            propertyLabel = getSelectedWidget(invisibleList);    
        }
        if (propertyLabel != null) {
            propertyLabel.setText(columnCaption != null && !columnCaption.isEmpty() ? columnCaption : propertyLabel.getPropertyItem().property.getNotEmptyCaption());
            propertyLabel.setUserCaption(columnCaption != null && !columnCaption.isEmpty() ? columnCaption : null);
        }
    }

    public void columnPatternBoxTextChanged(String columnPattern) {
        PropertyLabel propertyLabel = getSelectedWidget(visibleList);
        if (propertyLabel == null) {
            propertyLabel = getSelectedWidget(invisibleList);
        }
        if (propertyLabel != null) {
            propertyLabel.setUserPattern(columnPattern != null && !columnPattern.isEmpty() ? columnPattern : null);
        }
    }

    public ArrayList<Widget> getVisibleWidgets() {
        return visibleList.widgetList();
    }

    public ArrayList<Widget> getInvisibleWidgets() {
        return invisibleList.widgetList();
    }
    
    public int getVisibleCount() {
        return visibleList.getItemCount();
    }
    
    public int getVisibleIndex(Widget w) {
        return visibleList.widgetList().indexOf(w);
    }

    public int getInvisibleIndex(Widget w) {
        return invisibleList.widgetList().indexOf(w);
    }

    public void clearLists() {
        visibleList.clear();
        invisibleList.clear();
    }
}