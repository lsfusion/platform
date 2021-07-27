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

    protected final int columnsCount;
    protected final boolean alignCaptions;

    protected FlexPanel[] columns;
    protected FlexPanel[] captionColumns;
    protected List<AlignCaptionPanel> childrenCaptions;
    protected List<Integer> childrenCaptionBaseSizes;

    public LinearContainerView(GContainer container) {
        super(container);

        assert !container.isTabbed();

        columnsCount = container.columns;

        GFlexAlignment justifyContent = container.getFlexJustify(); // when there is free space (there is no non-zero flex)

        // later containers with explicit sizes can be included
        // plus also in simple containers we can wrap consecutive property views into some flexpanel, but it requires a lot more complex logics
        alignCaptions = container.isAlignCaptions();

        if(isSimple())
            panel = new FlexPanel(vertical, justifyContent);
        else {
            panel = new FlexPanel(!vertical);

            columns = new FlexPanel[columnsCount];
            captionColumns = new FlexPanel[columnsCount];
            childrenCaptions = new ArrayList<>();
            for (int i = 0; i < columnsCount; i++) {
                if(alignCaptions) {
                    FlexPanel captionColumn = new FlexPanel(vertical);
                    panel.add(captionColumn); // however it seems that GFlexAlignment.STRETCH is also possible
                    captionColumns[i] = captionColumn;
                }

                FlexPanel column = new FlexPanel(vertical, justifyContent);
                panel.addFillFlex(column, null); // we're using null flex basis to make columns behaviour similar to manually defined containers
                columns[i] = column;
            }
        }
    }

    public boolean isSimple() {
        return isSingleColumn() && !alignCaptions;
    }

    private boolean isSingleColumn() {
        return columnsCount == 1;
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

            child.installMargins(captionPanel); // need the same margins as property value

            childrenCaptions.add(index, captionPanel);
            ((GPropertyPanelController.Panel) view).captionContainer = (widget, valueSizes, alignment) -> {
                captionPanel.add(widget, alignment);
                Integer baseSize = vertical ? valueSizes.second : valueSizes.first;

                captionPanel.baseSize = baseSize; // it's called after it is first time added to the container, so we store it in some field for further adding, removing (actually it's needed for component "shifting", when we need to add/remove latter components)
                FlexPanel.setBaseSize(captionPanel, vertical, baseSize);  // oppositeAndFixed - false, since we're settings size for main direction
            };
        }

        if(isSingleColumn())
            addChildrenView(index, 0);
        else { // collections are already updated
            removeChildrenViews(index + 1, -1);
            addChildrenViews(index, 0);
        }
    }

    protected FlexPanel wrapBorderImpl(GComponent child) {
        GContainer childContainer;
        if(child instanceof GContainer && (childContainer = (GContainer) child).caption != null)
            return new CaptionPanel(childContainer.caption, vertical);
        return null;
    }

    @Override
    protected void removeImpl(int index, GComponent child) {
        if(isSingleColumn())
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
        int rowIndex = (index + offset) / columnsCount;
        int columnIndex = (index + offset) % columnsCount;

        add(isSimple() ? panel : columns[columnIndex], childrenViews.get(index), children.get(index), rowIndex);

        if(alignCaptions) {
            AlignCaptionPanel captionPanel = childrenCaptions.get(index);
            captionColumns[columnIndex].add(captionPanel, rowIndex, GFlexAlignment.START, 0, captionPanel.baseSize);
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
        int columnIndex = (index + offset) % columnsCount;

        (isSimple() ? panel : columns[columnIndex]).remove(childrenViews.get(index));

        if(alignCaptions)
            captionColumns[columnIndex].remove(childrenCaptions.get(index));
    }

    @Override
    public Dimension getMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        int size = children.size();

        int main = 0;
        int opposite = 0;

        if (size > 0) {
            int rows = (size - 1) / columnsCount + 1;
            for (int i = 0; i < columnsCount; i++) {
                int columnCross = 0;
                int columnMain = 0;
                int captionMain = 0;

                for (int j = 0; j < rows; j++) {
                    int index = i * columnsCount + j;
                    if(index < size) {
                        if(alignCaptions) {
                            Dimension captionPref = GwtClientUtils.calculateMaxPreferredSize(childrenCaptions.get(index));
                            captionMain = Math.max(captionMain, vertical ? captionPref.width : captionPref.height);
                        }

                        Dimension childPref = getChildMaxPreferredSize(containerViews, index);

                        GComponent child = children.get(index);
                        if(child instanceof GContainer && ((GContainer) child).caption != null) // adding border
                            childPref = getCaptionPanel((GContainer) child).adjustMaxPreferredSize(childPref);

                        columnMain = Math.max(columnMain, vertical ? childPref.width : childPref.height);
                        columnCross += vertical ? childPref.height : childPref.width; // captions cross is equal to columnCross
                    }
                }
                opposite = Math.max(opposite, columnCross);
                main += columnMain + captionMain;
            }
        }

        return new Dimension(vertical ? main : opposite, vertical ? opposite : main);
    }

    @Override
    public Widget getView() {
        return panel;
    }
}
