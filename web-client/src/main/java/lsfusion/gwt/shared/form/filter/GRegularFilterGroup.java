package lsfusion.gwt.shared.form.filter;

import lsfusion.gwt.shared.form.design.GComponent;
import lsfusion.gwt.shared.form.object.GGroupObject;

import java.util.ArrayList;

public class GRegularFilterGroup extends GComponent {
    public ArrayList<GRegularFilter> filters = new ArrayList<>();
    public int defaultFilterIndex;
    public GGroupObject groupObject;
//    public OrderedMap<ClientPropertyDraw, Boolean> nullOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
}
