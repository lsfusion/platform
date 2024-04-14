package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;

import java.util.ArrayList;

public class InlineComponentViewWidget implements ComponentViewWidget {

    private ArrayList<ComponentViewWidget> widgets = new ArrayList<>();
    private ArrayList<GFlexAlignment> alignments = new ArrayList<>();
    private ArrayList<Boolean> flexes = new ArrayList<>();
    private ArrayList<String> sIDs = new ArrayList<>();

    private final boolean vertical;

    public InlineComponentViewWidget(boolean vertical) {
        this.vertical = vertical;
    }

    public void add(SizedWidget widget, GFlexAlignment alignment, boolean flex, String sID) {
        widgets.add(widget.view);
        alignments.add(alignment);
        flexes.add(flex);
        sIDs.add(sID);
    }

    @Override
    public SizedWidget getSingleWidget() {
        return null;
    }

    @Override
    public Widget getShowingWidget() {
        return widgets.get(0).getShowingWidget();
    }

    @Override
    public void setShowIfVisible(boolean visible) {
        for(ComponentViewWidget widget : widgets)
            widget.setShowIfVisible(visible);
    }

    @Override
    public void setVisible(boolean visible) {
        for(ComponentViewWidget widget : widgets)
            widget.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return widgets.get(0).isVisible();
    }

    private String getInnerSID(String sID, int index) {
        String sInnerID = sIDs.get(index);
        if(!sInnerID.isEmpty())
            sID += "." + sInnerID;
        return sID;
    }

    @Override
    public void setDebugInfo(String sID) {
        for (int i = 0, widgetsSize = widgets.size(); i < widgetsSize; i++)
            widgets.get(i).setDebugInfo(getInnerSID(sID, i));
    }

    @Override
    public void attach(ResizableComplexPanel attachContainer) {
        for(ComponentViewWidget widget : widgets)
            widget.attach(attachContainer);
    }

    @Override
    public void replace(ResizableComplexPanel panel, String sID) {
        for (int i = 0, widgetsSize = widgets.size(); i < widgetsSize; i++)
            widgets.get(i).replace(panel, getInnerSID(sID, i));
    }

    @Override
    public void remove(ResizableComplexPanel panel) {
        for(ComponentViewWidget widget : widgets)
            widget.remove(panel);
    }

    @Override
    public void add(ResizableComplexPanel panel, int beforeIndex) {
        for(ComponentViewWidget widget : widgets)
            widget.add(panel, beforeIndex++);
    }

    @Override
    public void remove(ResizableComplexPanel panel, int containerIndex) {
        for(ComponentViewWidget widget : widgets)
            widget.remove(panel, containerIndex);
    }

    @Override
    public void add(SizedFlexPanel panel, int beforeIndex, GSize width, GSize height, double flex, boolean shrink, GFlexAlignment alignment, boolean alignShrink) {
        for (int i = 0, widgetsSize = widgets.size(); i < widgetsSize; i++) {
            ComponentViewWidget widget = widgets.get(i);
            GFlexAlignment childAlignment = alignments.get(i);
            boolean childFlex = flexes.get(i);

            widget.add(panel, beforeIndex++,
                    vertical && childAlignment.equals(GFlexAlignment.STRETCH) ? width : null,
                    !vertical && childAlignment.equals(GFlexAlignment.STRETCH) ? height : null,
                    childFlex ? flex : 0,
                    childFlex ? shrink : false,
                    alignment.equals(GFlexAlignment.STRETCH) ? childAlignment : alignment,
                    alignment.equals(GFlexAlignment.STRETCH) ? childAlignment.isShrink() : alignShrink);
        }
    }

    @Override
    public void remove(SizedFlexPanel panel, int containerIndex) {
        for(ComponentViewWidget widget : widgets)
            widget.remove(panel, containerIndex);
    }

    @Override
    public void remove(SizedFlexPanel panel) {
        for(ComponentViewWidget widget : widgets)
            widget.remove(panel);
    }

    @Override
    public int getWidgetCount() {
        return widgets.size();
    }
}
