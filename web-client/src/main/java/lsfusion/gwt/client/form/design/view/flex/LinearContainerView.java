package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.CaptionPanel;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LinearContainerView extends GAbstractContainerView {

    protected final FlexPanel panel;

    protected final int linesCount;
    protected final boolean alignCaptions;
    protected final boolean grid;

    protected FlexPanel[] lines;
    protected FlexPanel[] captionLines;
    protected List<AlignCaptionPanel> childrenCaptions;
    protected List<Integer> childrenCaptionBaseSizes;

    private final static FlexPanel.FlexLayoutData captionLine = new FlexPanel.FlexLayoutData(0, null);
    private final static FlexPanel.FlexLayoutData simpleLine = new FlexPanel.FlexLayoutData(1, null);

    public static FlexPanel.FlexLayoutData[] getLineFlexLayouts(boolean alignCaptions, int linesCount) {
        int alignDiv = alignCaptions ? 2 : 1;
        int alignOff = alignCaptions ? 1 : 0;

        FlexPanel.FlexLayoutData[] result = new FlexPanel.FlexLayoutData[linesCount * alignDiv];
        for(int i = 0; i < linesCount; i++)
            result[alignDiv * i + alignOff] = simpleLine;

        if(alignCaptions)
            for(int i = 0; i < linesCount; i++)
                result[alignDiv * i] = captionLine;
        return result;
    }

    public LinearContainerView(GContainer container) {
        super(container);

        assert !container.tabbed;

        linesCount = container.lines;

        GFlexAlignment flexAlignment = container.getFlexAlignment(); // when there is free space (there is no non-zero flex)

        // later containers with explicit sizes can be included
        // plus also in simple containers we can wrap consecutive property views into some flexpanel, but it requires a lot more complex logics
        alignCaptions = container.isAlignCaptions();
        grid = container.isGrid();

        if(alignCaptions)
            childrenCaptions = new ArrayList<>();

        if(isSingleLine())
            panel = new FlexPanel(vertical, flexAlignment, grid || alignCaptions ? getLineFlexLayouts(alignCaptions, linesCount) : null);
        else {
            panel = new FlexPanel(!vertical);
            // we don't want this panel to be resized, because we don't set overflow, and during resize container can get fixed size (and then if inner container resized it's content overflows outer border)
            // however resizing inner component also causes troubles, because when you increase components base size, parent components base size also is changed which leads to immediate relayouting, and if the explicit base size is larger than auto base size, there is a leap
            // plus in that case line resizing is not that ergonomic, because it can be shrinked if you resize a component different from the component you used to extend the line
            // so it seems that having childrenResizable true is the lesser evil
//            panel.childrenResizable = false;

            Integer lineSize = container.getLineSize();

            lines = new FlexPanel[linesCount];
            for (int i = 0; i < linesCount; i++) {
                FlexPanel line = new FlexPanel(vertical, flexAlignment, alignCaptions ? getLineFlexLayouts(true, 1) : null);
                panel.addFillFlex(line, lineSize); // we're using null flex basis to make lines behaviour similar to manually defined containers
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
    public void updateCaption(GContainer container) {
        getCaptionPanel(container).setCaption(container.caption);
    }

    public CaptionPanel getCaptionPanel(GContainer container) {
        FlexPanel childPanel = (FlexPanel) getChildView(container);

        // if we have caption it has to be either FlexCaptionPanel, or it is wrapped into one more flexPanel (see addImpl)
        CaptionPanel caption;
        if(childPanel instanceof CaptionPanel)
            caption = (CaptionPanel) childPanel;
        else
            caption = (CaptionPanel) childPanel.getWidget(0);
        return caption;
    }

    private static class AlignCaptionPanel extends FlexPanel {
        public AlignCaptionPanel(boolean vertical, GFlexAlignment flexAlignment) {
            super(vertical, flexAlignment);
        }
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        if(alignCaptions) { // when adding GPropertyPanelController.Panel is empty, so we have to do everything wit callback
            AlignCaptionPanel captionPanel;
            if(child.isAlignCaption()) { // from alignCaptions
                // need a wrapper since all elements in grid have justify-items STRETCH by default (and we want CENTERED alignment)
                // plus captionContainer is filled later (after it has to be added to DOM)
                captionPanel = new AlignCaptionPanel(!vertical, ((CaptionContainerHolder) view).getCaptionHAlignment());
                captionPanel.addStyleName("dataPanelRendererPanel"); // just like in PanelRenderer for no-wrap

                ((CaptionContainerHolder) view).setCaptionContainer((columnCaptionWidget, alignment) -> {
                    assert vertical; // because of alignCaptions first check (isVertical())
                    captionPanel.add(columnCaptionWidget, alignment);
                });
            } else
                captionPanel = null;

            childrenCaptions.add(index, captionPanel);
        }

        if(isSingleLine())
            addChildrenView(index, 0);
        else { // collections are already updated
            removeChildrenViews(index + 1, -1);
            addChildrenViews(index, 0);
        }
    }

    protected FlexPanel wrapBorderImpl(GComponent child) {
        GContainer childContainer;
        if(child instanceof GContainer && (childContainer = (GContainer) child).caption != null)
            return new CaptionPanel(childContainer.caption);
        return null;
    }

    @Override
    protected void removeImpl(int index, GComponent child) {
        if(isSingleLine())
            removeChildrenView(index, 0);
        else { // collections are not yet updated
            removeChildrenViews(index, 0);
            addChildrenViews(index + 1,  -1);
        }

        if(alignCaptions)
            childrenCaptions.remove(index);
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
        for(int i = 0; i < index; )
            if(panel.getWidget(w++) instanceof AlignCaptionPanel)
                offset++;
            else
                i++;
        return offset;
    }

    private Pair<FlexPanel, Integer> getContainerPosition(int index) {
        int containerIndex;
        FlexPanel container;
        if(isSingleLine()) {
            container = panel;
            containerIndex = index;
        } else {
            int lineIndex = index % linesCount;
            container = lines[lineIndex];
            containerIndex = index / linesCount;
        }
        return new Pair<>(container, containerIndex + (alignCaptions ? getCaptionOffset(container, containerIndex) : 0));
    }

    private void addChildrenView(int index, int offset) {
        Pair<FlexPanel, Integer> containerPosition = getContainerPosition(index + offset);
        FlexPanel container = containerPosition.first;
        int containerIndex = containerPosition.second;

        Widget widget = addChildrenWidget(container, index, containerIndex);

        int span = 1;
        if(grid)
            span = children.get(index).getSpan();

        if(alignCaptions) {
            span = span * 2;

            AlignCaptionPanel captionPanel = childrenCaptions.get(index);
            if(captionPanel != null) {
                container.add(captionPanel, containerIndex, GFlexAlignment.STRETCH, 0, null);
                span--;
            }
        }

        if(span > 1)
            FlexPanel.setSpan(widget, span, !vertical);
    }

    private void removeChildrenView(int index, int offset) {
        Pair<FlexPanel, Integer> containerPosition = getContainerPosition(index + offset);
        FlexPanel container = containerPosition.first;
        int containerIndex = containerPosition.second;

        if(alignCaptions)
            if(childrenCaptions.get(index) != null)
                container.remove(containerIndex);

        container.remove(containerIndex);
    }

    @Override
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        for (int i = 0, size = children.size(); i < size; i++) {
            GComponent child = children.get(i);
            if(child instanceof GContainer) // optimization
                childrenViews.get(i).setVisible(childrenVisible[i]);
        }

        super.updateLayout(requestIndex, childrenVisible);
    }

    @Override
    public Dimension getMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        int size = children.size();

        int main = 0;
        int opposite = 0;

        if (size > 0) {
            int rows = (size - 1) / linesCount + 1;
            for (int i = 0; i < linesCount; i++) {
                int lineCross = 0;
                int lineMain = 0;
                int captionMain = 0;

                for (int j = 0; j < rows; j++) {
                    int index = j * linesCount + i;
                    if(index < size) {
                        if(alignCaptions) {
                            AlignCaptionPanel captionPanel = childrenCaptions.get(index);
                            if(captionPanel != null) {
                                Dimension captionPref = GwtClientUtils.calculateMaxPreferredSize(captionPanel);
                                captionMain = Math.max(captionMain, vertical ? captionPref.width : captionPref.height);
                            }
                        }

                        Dimension childPref = getChildMaxPreferredSize(containerViews, index);

                        GComponent child = children.get(index);
                        if(child instanceof GContainer && ((GContainer) child).caption != null) // adding border
                            childPref = getCaptionPanel((GContainer) child).adjustMaxPreferredSize(childPref);

                        lineMain = Math.max(lineMain, vertical ? childPref.width : childPref.height);
                        lineCross += vertical ? childPref.height : childPref.width; // captions cross is equal to lineCross
                    }
                }
                opposite = Math.max(opposite, lineCross);
                main += lineMain + captionMain;
            }
        }

        return new Dimension(vertical ? main : opposite, vertical ? opposite : main);
    }

    @Override
    public Widget getView() {
        return panel;
    }
}
