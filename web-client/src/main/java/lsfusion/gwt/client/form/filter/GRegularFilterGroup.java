package lsfusion.gwt.client.form.filter;

import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObject;

import java.util.ArrayList;

public class GRegularFilterGroup extends GComponent {
    public ArrayList<GRegularFilter> filters = new ArrayList<>();
    public int defaultFilterIndex;
    public GGroupObject groupObject;
    public boolean noNull;
}
