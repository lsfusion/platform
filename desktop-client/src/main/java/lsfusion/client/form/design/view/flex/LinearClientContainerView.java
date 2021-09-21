package lsfusion.client.form.design.view.flex;

import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.AbstractClientContainerView;
import lsfusion.client.form.design.view.CaptionPanel;
import lsfusion.client.form.design.view.ClientContainerView;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.client.form.object.panel.controller.PropertyPanelController;
import lsfusion.interop.base.view.FlexAlignment;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LinearClientContainerView extends AbstractClientContainerView {

    protected final FlexPanel panel;

    protected final int linesCount;
    protected final boolean alignCaptions;

    protected FlexPanel[] lines;
    protected FlexPanel[] captionLines;
    protected List<AlignCaptionPanel> childrenCaptions;

    public LinearClientContainerView(ClientContainer container) {
        super(container);

        assert !container.isTabbed();

        linesCount = container.lines;

        FlexAlignment justifyContent = container.getFlexJustify(); // when there is free space (there is no non-zero flex)

        // later containers with explicit sizes can be included
        // plus also in simple containers we can wrap consecutive property views into some flexpanel, but it requires a lot more complex logics
        alignCaptions = container.isAlignCaptions();

        if(isSimple())
            panel = new FlexPanel(vertical, justifyContent);
        else {
            panel = new FlexPanel(!vertical, FlexAlignment.START);

            lines = new FlexPanel[linesCount];
            captionLines = new FlexPanel[linesCount];
            childrenCaptions = new ArrayList<>();
            for (int i = 0; i < linesCount; i++) {
                if(alignCaptions) {
                    FlexPanel captionLine = new FlexPanel(vertical, justifyContent);
                    panel.add((Widget)captionLine, FlexAlignment.STRETCH); // however it seems that FlexAlignment.STRETCH is also possible
                    captionLines[i] = captionLine;
                }

                FlexPanel line = new FlexPanel(vertical, justifyContent);
                panel.addFillFlex(line, null); // we're using null flex basis to make lines behaviour similar to manually defined containers
                lines[i] = line;
            }
        }

        ClientColorUtils.designComponent(panel, container.design);
    }

    public boolean isSimple() {
        return isSingleLine() && !alignCaptions;
    }

    private boolean isSingleLine() {
        return linesCount == 1;
    }

    private static class AlignCaptionPanel extends FlexPanel {
        public AlignCaptionPanel(boolean vertical) {
            super(vertical);
        }

        public Integer baseSize;
    }

    public void updateCaption(ClientContainer clientContainer) {
        getCaptionPanel(clientContainer).setCaption(clientContainer.caption);
    }

    public CaptionPanel getCaptionPanel(ClientContainer container) {
        Widget childPanel = getChildView(container);

        CaptionPanel caption = null;
        if(childPanel instanceof CaptionPanel)
            caption = (CaptionPanel) childPanel;
//        else // we don't need this since we don't wrap anything like in desktop client
//            caption = (CaptionPanel) childPanel.getWidget(0);
        return caption;
    }

    @Override
    public void addImpl(int index, ClientComponent child, Widget view) {
        if (alignCaptions) { // when adding PropertyPanelController.Panel is empty, so we have to do everything wit callback
            AlignCaptionPanel captionPanel = new AlignCaptionPanel(!vertical);
            captionPanel.setDebugContainer(wrapDebugContainer("CAPTION", view));

            child.installMargins(captionPanel); // need the same margins as property value

            childrenCaptions.add(index, captionPanel);
            ((PropertyPanelController.Panel) view).captionContainer = (widget, valueSizes, alignment) -> {
                assert vertical; // because of aligncaptions first check (isVertical())
                captionPanel.add(widget, alignment);

                Integer baseSize = vertical ? valueSizes.second : valueSizes.first;

                Integer size = child.getSize(vertical);
                if (size != null)
                    baseSize = size;

                captionPanel.baseSize = baseSize; // it's called after it is first time added to the container, so we store it in some field for further adding, removing (actually it's needed for component "shifting", when we need to add/remove latter components)
                // oppositeAndFixed - null, since we're settings size for main direction
                FlexPanel.setBaseSize(captionPanel, vertical, baseSize);
            };
        }

        if(isSingleLine())
            addChildrenView(index, 0);
        else { // collections are already updated
            removeChildrenViews(index + 1, -1);
            addChildrenViews(index, 0);
        }
    }

    @Override
    protected FlexPanel wrapBorderImpl(ClientComponent child) {
        ClientContainer childContainer;
        if(child instanceof ClientContainer && (childContainer = (ClientContainer) child).caption != null)
            return new CaptionPanel(childContainer.caption, vertical);
        return null;
    }

    @Override
    public void removeImpl(int index, ClientComponent child) {
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
        for (int index = startFrom, size = children.size(); index < size; index++)
            removeChildrenView(index, offset);
    }

    private void addChildrenView(int index, int offset) {
        int rowIndex = (index + offset) / linesCount;
        int lineIndex = (index + offset) % linesCount;

        add(isSimple() ? panel : lines[lineIndex], childrenViews.get(index), children.get(index), rowIndex);

        if(alignCaptions) {
            AlignCaptionPanel captionPanel = childrenCaptions.get(index);
            captionLines[lineIndex].add(captionPanel, rowIndex, FlexAlignment.START, 0, captionPanel.baseSize);
        }
    }

    private void removeChildrenView(int index, int offset) {
        int lineIndex = (index + offset) % linesCount;

        (isSimple() ? panel : lines[lineIndex]).remove(childrenViews.get(index));

        if(alignCaptions)
            captionLines[lineIndex].remove((Widget) childrenCaptions.get(index));
    }

    @Override
    public void updateLayout(boolean[] childrenVisible) {
        for (int i = 0, size = children.size(); i < size; i++) {
            ClientComponent child = children.get(i);
            if(child instanceof ClientContainer) // optimization
                childrenViews.get(i).setVisible(childrenVisible[i]);
        }

        super.updateLayout(childrenVisible);
    }

    @Override
    public Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews) {
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
                            Dimension captionPref = calculateMaxPreferredSize(childrenCaptions.get(index));
                            captionMain = Math.max(captionMain, vertical ? captionPref.width : captionPref.height);
                        }

                        Dimension childPref = getChildMaxPreferredSize(containerViews, index);

                        ClientComponent child = children.get(index);
                        if(child instanceof ClientContainer && ((ClientContainer) child).caption != null) // adding border
                            childPref = getCaptionPanel((ClientContainer) child).adjustMaxPreferredSize(childPref);

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
