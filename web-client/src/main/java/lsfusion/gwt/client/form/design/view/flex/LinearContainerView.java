package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.CaptionWidget;

public class LinearContainerView extends LayoutContainerView {

    protected final SizedFlexPanel panel;

    protected final int linesCount;
    protected final boolean grid;

    protected SizedFlexPanel[] lines;

    public static FlexPanel.GridLines getLineGridLayouts(boolean alignCaptions, GSize lineSize, GSize captionLineSize, int linesCount, boolean wrap, boolean lineShrink) {
        FlexPanel.FlexLayoutData valueLine = new FlexPanel.FlexLayoutData(1, lineSize, lineShrink);

        if(wrap) {
            assert !alignCaptions;
            return new FlexPanel.GridWrapLines(valueLine);
        }

        int alignDiv = alignCaptions ? 2 : 1;
        int alignOff = alignCaptions ? 1 : 0;

        FlexPanel.FlexLayoutData[] result = new FlexPanel.FlexLayoutData[linesCount * alignDiv];
        for(int i = 0; i < linesCount; i++)
            result[alignDiv * i + alignOff] = valueLine;

        if(alignCaptions)
            for(int i = 0; i < linesCount; i++)
                result[alignDiv * i] = new FlexPanel.FlexLayoutData(0, captionLineSize, false);
        return new FlexPanel.GridFixedLines(result);
    }

    public LinearContainerView(GFormController formController, GContainer container) {
        super(container, formController);

        linesCount = container.lines;

        GFlexAlignment flexAlignment = container.getFlexAlignment(); // when there is free space (there is no non-zero flex)

        // later containers with explicit sizes can be included
        // plus also in simple containers we can wrap consecutive property views into some flexpanel, but it requires a lot more complex logics
        grid = container.isGrid();
        boolean wrap = container.isWrap();

        GSize lineSize = container.getLineSize();
        GSize captionLineSize = container.getCaptionLineSize();
        boolean lineShrink = container.isLineShrink();

        if(isSingleLine()) {
            panel = new SizedFlexPanel(vertical, flexAlignment, grid || alignCaptions ? getLineGridLayouts(alignCaptions, lineSize, captionLineSize, linesCount, wrap, lineShrink) : null, wrap);
        } else {
            panel = new SizedFlexPanel(!vertical, GFlexAlignment.START, null, vertical && wrap);

            // we don't want this panel to be resized, because we don't set overflow, and during resize container can get fixed size (and then if inner container resized it's content overflows outer border)
            // however resizing inner component also causes troubles, because when you increase components base size, parent components base size also is changed which leads to immediate relayouting, and if the explicit base size is larger than auto base size, there is a leap
            // plus in that case line resizing is not that ergonomic, because it can be shrinked if you resize a component different from the component you used to extend the line
            // so it seems that having childrenResizable true is the lesser evil
//            panel.childrenResizable = false;

            lines = new SizedFlexPanel[linesCount];
            for (int i = 0; i < linesCount; i++) {
                SizedFlexPanel line = new SizedFlexPanel(vertical, flexAlignment, alignCaptions ? getLineGridLayouts(true, lineSize, captionLineSize, 1, false, lineShrink) : null, !vertical && wrap); // in theory true can be used instead of lineShrink

                panel.add(line, GFlexAlignment.STRETCH, 1, lineShrink, null);
                lines[i] = line;

                if (lineSize != null) // because of non-null flex-basis column won't take content size which may then overflow over column
                    line.getElement().getStyle().setOverflow(Style.Overflow.AUTO);
            }
        }
    }

    private boolean isSingleLine() {
        return linesCount == 1 || grid;
    }

    @Override
    protected void addImpl(int index) {
        if(isSingleLine())
            addChildrenView(index, 0);
        else { // collections are already updated
            removeChildrenViews(index + 1, -1);
            addChildrenViews(index, 0);
        }
    }

    @Override
    protected void removeImpl(int index, GComponent child) {
        if(isSingleLine())
            removeChildrenView(index, 0);
        else { // collections are not yet updated
            removeChildrenViews(index, 0);
            addChildrenViews(index + 1,  -1);
        }
    }

    private void addChildrenViews(int startFrom, int offset) {
        for (int index = startFrom, size = children.size(); index < size; index++)
            addChildrenView(index, offset);
    }

    private void removeChildrenViews(int startFrom, int offset) {
        for (int index = children.size() - 1; index >= startFrom; index--)
            removeChildrenView(index, offset);
    }

    private int getCaptionOffset(FlexPanel panel, int index) {
        int offset = 0;
        int w = 0;
        for(int i = 0; i < index; ) {
            Widget widget = panel.getWidget(w++);
            if(((FlexPanel.WidgetLayoutData)widget.getLayoutData()).caption)
                offset++;
            else
                i++;
        }
        return offset;
    }

    private Pair<SizedFlexPanel, Integer> getContainerPosition(int index) {
        int containerIndex;
        SizedFlexPanel container;
        if(isSingleLine()) {
            container = panel;
            containerIndex = index;
        } else {
            int lineIndex = index % linesCount;
            container = lines[lineIndex];
            containerIndex = index / linesCount;
        }
        assert container.isVertical() == vertical;
        return new Pair<>(container, containerIndex + (alignCaptions ? getCaptionOffset(container, containerIndex) : 0));
    }

    private void addChildrenView(int index, int offset) {
        Pair<SizedFlexPanel, Integer> containerPosition = getContainerPosition(index + offset);
        SizedFlexPanel container = containerPosition.first;
        int containerIndex = containerPosition.second;

        Widget widget = addChildrenWidget(container, index, containerIndex);

        int span = 1;
        if(grid)
            span = children.get(index).getSpan();

        if(alignCaptions) {
            assert container.isGrid();
            span = span * 2;

            CaptionWidget captionPanel = childrenCaptions.get(index);
            if(captionPanel != null) {
                SizedWidget captionSizedWidget = captionPanel.widget;
                Widget captionWidget = captionSizedWidget.widget;

                boolean vertical = container.isVertical();
                captionSizedWidget.add(container, containerIndex, vertical ? captionPanel.horzAlignment : captionPanel.vertAlignment);
                FlexPanel.setGridAlignment(captionWidget.getElement(), vertical, vertical ? captionPanel.vertAlignment : captionPanel.horzAlignment);
                ((FlexPanel.WidgetLayoutData)captionWidget.getLayoutData()).caption = true;
                span--;
            }
        }

        if(span > 1)
            FlexPanel.setSpan(widget, span, !vertical);
    }

    private void removeChildrenView(int index, int offset) {
        Pair<SizedFlexPanel, Integer> containerPosition = getContainerPosition(index + offset);
        SizedFlexPanel container = containerPosition.first;
        int containerIndex = containerPosition.second;

        if(alignCaptions)
            if(childrenCaptions.get(index) != null)
                container.removeSized(containerIndex);

        container.removeSized(containerIndex);
    }

    @Override
    public Widget getView() {
        return panel;
    }
}
