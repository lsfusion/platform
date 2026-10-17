package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.view.GTableView;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.List;

// the view of a GRID's rows, one of the list views its toolbar switches (GListViewType): the grid itself, a pivot, a
// map, a calendar, a custom view - what a table's view is (GTableView), and what a grid asks of it besides
public interface GListView extends GTableView {
    
    // components
    default Widget getThisWidget() { return (Widget)this; }
    default Widget getWidget() {
        return getThisWidget();
    }

    // SETTERS
    // keys
    void setKeys(ArrayList<GGroupObjectValue> keys);

    // event - FINISH SETTER
    void update(Boolean updateState);

    // GETTERS (editing / toolbar features)
    // columns
    GPropertyDraw getCurrentProperty(); // calculate sum / filtering default value
    GGroupObjectValue getCurrentColumnKey(); // calculate sum / filtering default value

    boolean isNoColumns(); // hide if there're no columns after update
    long getSetRequestIndex(); // we need to understand that view was already updated, to avoid unnecessary effects (for example making grid invisible)
    void setSetRequestIndex(long index); // we need to understand that view was already updated, to avoid unnecessary effects (for example making grid invisible)

    // focus
    default void focus(FocusUtils.Reason reason) {
        FocusUtils.focus(getTableDataFocusElement(), reason);
    }
    Element getTableDataFocusElement(); // protected

    // toolbar features
    void runGroupReport();
    List<Pair<Column, String>> getFilterColumns(); // for filter to get all columns with keys and captions
    PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey); // for filter to set default value

    boolean hasUserPreferences();
    GGroupObjectUserPreferences getCurrentUserGridPreferences();
    GGroupObjectUserPreferences getGeneralGridPreferences();

    int getPageSize();

    default void updateRecordLayout(long requestIndex) {}

    default void onRender(Event editEvent){}
}
