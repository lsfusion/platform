package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.CaptionPanel;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.object.panel.controller.GPropertyPanelController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LinearContainerView extends GAbstractContainerView {

    protected final FlexPanel panel;

    protected final int linesCount;
    protected final boolean alignCaptions;

    protected FlexPanel[] lines;
    protected FlexPanel[] captionLines;
    protected List<AlignCaptionPanel> childrenCaptions;
    protected List<Integer> childrenCaptionBaseSizes;

    public LinearContainerView(GContainer container) {
        super(container);

        assert !container.tabbed;

        linesCount = container.lines;

        GFlexAlignment flexAlignment = container.getFlexAlignment(); // when there is free space (there is no non-zero flex)

        // later containers with explicit sizes can be included
        // plus also in simple containers we can wrap consecutive property views into some flexpanel, but it requires a lot more complex logics
        alignCaptions = container.isAlignCaptions();

        if(isSimple())
            panel = new FlexPanel(vertical, flexAlignment);
        else {
            panel = new FlexPanel(!vertical);
            // we don't want this panel to be resized, because we don't set overflow, and during resize container can get fixed size (and then if inner container resized it's content overflows outer border)
            // however resizing inner component also causes troubles, because when you increase components base size, parent components base size also is changed which leads to immediate relayouting, and if the explicit base size is larger than auto base size, there is a leap
            // plus in that case line resizing is not that ergonomic, because it can be shrinked if you resize a component different from the component you used to extend the line
            // so it seems that having childrenResizable true is the lesser evil
//            panel.childrenResizable = false;

            lines = new FlexPanel[linesCount];
            captionLines = new FlexPanel[linesCount];
            childrenCaptions = new ArrayList<>();
            for (int i = 0; i < linesCount; i++) {
                if(alignCaptions) {
                    FlexPanel captionLine = new FlexPanel(vertical, flexAlignment);
                    panel.add(captionLine, GFlexAlignment.STRETCH); // we need the same alignment as used for the "main" line (it's important if justifyContent is used)
                    captionLines[i] = captionLine;
                }

                FlexPanel line = new FlexPanel(vertical, flexAlignment);
                panel.addFillFlex(line, null); // we're using null flex basis to make lines behaviour similar to manually defined containers
                lines[i] = line;
            }
        }
    }

    public boolean isSimple() {
        return isSingleLine() && !alignCaptions;
    }

    private boolean isSingleLine() {
        return linesCount == 1;
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
        public AlignCaptionPanel(boolean vertical) {
            super(vertical);
        }

        public Integer baseSize;
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        if(alignCaptions) { // when adding GPropertyPanelController.Panel is empty, so we have to do everything wit callback
            AlignCaptionPanel captionPanel = new AlignCaptionPanel(!vertical);
            captionPanel.addStyleName("dataPanelRendererPanel"); // just like in PanelRenderer for no-wrap

//            child.installMargins(captionPanel); // need the same margins as property value

            childrenCaptions.add(index, captionPanel);
            ((GPropertyPanelController.Panel) view).captionContainer = (columnCaptionWidget, actualCaptionWidget, valueSizes, alignment) -> {
                assert vertical; // because of aligncaptions first check (isVertical())
                captionPanel.add(columnCaptionWidget, alignment);

                Integer baseSize = vertical ? valueSizes.second : valueSizes.first;

                Integer size = child.getSize(vertical);
                if (size != null)
                    baseSize = size;

                if(actualCaptionWidget == null) {
                    captionPanel.baseSize = baseSize; // this code line is called after captionPanel is first time added to the container, so we store it in some field for further adding, removing (actually it's needed ONLY for component "shifting", when we component is removed and later is added once again)
                    actualCaptionWidget = captionPanel;
                }
                actualCaptionWidget.addStyleName("alignPanelLabel");
                FlexPanel.setBaseSize(actualCaptionWidget, vertical, baseSize);  // oppositeAndFixed - false, since we're setting the size for the main direction
            };
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

    private void addChildrenView(int index, int offset) {
        int rowIndex = (index + offset) / linesCount;
        int lineIndex = (index + offset) % linesCount;

        addChildrenWidget(isSimple() ? panel : lines[lineIndex], index, rowIndex);

        if(alignCaptions) {
            AlignCaptionPanel captionPanel = childrenCaptions.get(index);
            captionLines[lineIndex].add(captionPanel, rowIndex, GFlexAlignment.START, 0, captionPanel.baseSize);
        }
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

    private void removeChildrenView(int index, int offset) {
        int rowIndex = (index + offset) / linesCount;
        int lineIndex = (index + offset) % linesCount;

        (isSimple() ? panel : lines[lineIndex]).remove(rowIndex);

        if(alignCaptions)
            captionLines[lineIndex].remove(rowIndex);
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
                            Dimension captionPref = GwtClientUtils.calculateMaxPreferredSize(childrenCaptions.get(index));
                            captionMain = Math.max(captionMain, vertical ? captionPref.width : captionPref.height);
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
