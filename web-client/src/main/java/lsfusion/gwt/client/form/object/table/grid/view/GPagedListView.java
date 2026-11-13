package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.view.DivWidget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.StaticImageButton;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GPlatformGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.view.MainFrame;

// a list view that is no table of the platform's: it draws a page of the group's rows, as many as the page size, in
// an element of its own (getDrawElement) - a pivot, a map, a calendar, a custom view, from the state they keep
// (GStateTableView), or a React component, from what the projection keeps (GReactCustom). What such a view shows
// besides what it draws: the notice that the page is full, with its "show all", and the shade over what is drawn
// while an update is awaited
// flexpanel, since we need to add pagesize widget + attach it to handle events
public abstract class GPagedListView extends FlexPanel implements GListView {

    protected final GFormController form;
    protected final GPlatformGridController grid;

    private long setRequestIndex;

    private final TableContainer tableContainer;

    public GPagedListView(GFormController form, GPlatformGridController grid, TableContainer tableContainer) {
        super(true);

        this.form = form;
        this.grid = grid;

        this.tableContainer = tableContainer;

//        setElement(DOM.createDiv());

        drawWidget = new DivWidget();
        addFill(drawWidget);

        initPageSizeWidget();

        GFormController.setBindingGroupObject(this, grid.groupObject);
    }

    private final Label messageLabel = new Label();

    public void initPageSizeWidget() {
        FlexPanel messageAndButton = new FlexPanel();
        messageLabel.getElement().getStyle().setPaddingRight(4, Style.Unit.PX);
        messageAndButton.addCentered(messageLabel);

        StaticImageButton showAllButton = new StaticImageButton(ClientMessages.Instance.get().formGridPageSizeShowAll(), null);
        showAllButton.addClickHandler(event -> {
            updateRendererState(true);
            pageSize = Integer.MAX_VALUE / 10; // /10 to prevent Integer overflow because in GroupObjectInstance we use "pageSize * 2"
            showAllPressed();
            this.grid.changePageSize(pageSize);
        });
        messageAndButton.addCentered(showAllButton);

        FlexPanel centeredMessageAndButton = new FlexPanel(true);
        centeredMessageAndButton.addCentered(messageAndButton);
        centeredMessageAndButton.getElement().getStyle().setPadding(2, Style.Unit.PX);

        this.pageSizeWidget = centeredMessageAndButton;
        this.pageSizeWidget.setVisible(false);

        ResizableSimplePanel child = new ResizableSimplePanel();
        child.setWidget(this.pageSizeWidget);
        addStretched(child); // we need to attach pageSize widget to make it work
//
//        add(new ResizableSimplePanel(this.pageSizeWidget)); // we need to attach pageSize widget to make it work
    }

    protected void showAllPressed() {
    }

    private final Widget drawWidget;
    public Element getDrawElement() {
        return drawWidget.getElement();
    }

    public Widget getPopupOwnerWidget() {
        return getWidget();
    }

    public Element getTableDataFocusElement() {
        return tableContainer.getFocusElement();
    }

    private Widget pageSizeWidget;
    protected Widget getPageSizeWidget() {
        return pageSizeWidget;
    }

    private int pageSize = getDefaultPageSize();

    // should correspond FormInstance.constructor - changePageSize method
    public int getDefaultPageSize() {
        return 1000;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    // the rows the view has
    protected abstract int getRowCount();

    protected boolean isPageSizeHit() {
        return getRowCount() >= getPageSize();
    }

    protected void updatePageSizeState(boolean hit) {
        messageLabel.setText(ClientMessages.Instance.get().formGridPageSizeHit(getRowCount())); //need to show current objects size
        getPageSizeWidget().setVisible(hit);
    }
    protected abstract Element getRendererAreaElement();

    private boolean updateState;
    // what it shows besides what it draws, once that is drawn: the notice, and the shade as the server's answer says
    protected void updateShown(Boolean updateState) {
        if(updateState != null)
            this.updateState = updateState;

        updatePageSizeState(isPageSizeHit());
        updateRendererState(this.updateState); // update state with server response
    }

    private long lastRendererDropped = 0;
    protected void updateRendererState(boolean set) {
        Runnable setFilter = () -> setOpacity(set, getRendererAreaElement());

        if(set) {
            long wasRendererDropped = lastRendererDropped;
            Scheduler.get().scheduleFixedDelay(() -> {
                if(wasRendererDropped == lastRendererDropped) // since set and drop has different timeouts
                    setFilter.run();
                return false;
            }, (int) MainFrame.updateRendererStateSetTimeout);
        } else {
            lastRendererDropped++;
            setFilter.run();
        }
    }

    protected static void setOpacity(boolean updateState, Element element) {
        if (updateState) {
            element.getStyle().setProperty("filter", "opacity(0.5)");
        } else {
            //there is a bug with position:fixed and opacity parameter
            element.getStyle().setProperty("filter", "");
        }
    }

    @Override
    public long getSetRequestIndex() {
        return setRequestIndex;
    }

    @Override
    public void setSetRequestIndex(long index) {
        setRequestIndex = index;
    }

    // ignore for now
    @Override
    public void focusProperty(GPropertyDraw propertyDraw) {
        focus(FocusUtils.Reason.ACTIVATE);
    }

    @Override
    public void runGroupReport() {
    }

    @Override
    public boolean hasUserPreferences() {
        return false;
    }

    @Override
    public GGroupObjectUserPreferences getCurrentUserGridPreferences() {
        return null;
    }

    @Override
    public GGroupObjectUserPreferences getGeneralGridPreferences() {
        return null;
    }
}
