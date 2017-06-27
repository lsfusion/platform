package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.client.form.ui.layout.SplitPanelBase;
import lsfusion.gwt.form.shared.view.GComponent;

import java.util.ArrayList;
import java.util.List;

public class FlexSplitPanel extends SplitPanelBase<FlexPanel> {

    private GComponent firstChild;
    private GComponent secondChild;

    public FlexSplitPanel(boolean vertical) {
        super(vertical, new Panel(vertical));
        ((Panel)panel).setSplitPanel(this);
    }

    @Override
    protected void addSplitterImpl(Splitter splitter) {
        panel.add(splitter, GFlexAlignment.STRETCH);
    }

    @Override
    protected void addFirstWidgetImpl(GComponent child, Widget widget, double flex1) {
        firstChild = child;
        panel.add(firstWidget, 0, GFlexAlignment.STRETCH, flex1);
        Style style = widget.getElement().getStyle();
        style.setOverflowY(vertical ? Style.Overflow.AUTO : Style.Overflow.HIDDEN);
        style.setOverflowX(vertical ? Style.Overflow.HIDDEN : Style.Overflow.AUTO);
    }

    @Override
    protected void addSecondWidgetImpl(GComponent child, Widget widget, double flex2) {
        secondChild = child;
        panel.add(widget, firstWidget == null ? 1 : 2, GFlexAlignment.STRETCH, flex2);
        Style style = widget.getElement().getStyle();
        style.setOverflowY(vertical ? Style.Overflow.AUTO : Style.Overflow.HIDDEN);
        style.setOverflowX(vertical ? Style.Overflow.HIDDEN : Style.Overflow.AUTO);
    }

    private static class LocationData {
        private final double availableSize;
        private final double size;
        private final double flex;

        public LocationData(double availableSize, double size, double flex) {
            this.availableSize = availableSize;
            this.size = size;
            this.flex = flex;
        }
    }

    private List<LocationData> prevLocations = new ArrayList<>();

    @Override
    protected void setSplitSize(double ratio, double flexSum, boolean recheck) {
        double availableSize = getAvailableSize();

        boolean firstVisible = firstWidget != null && firstWidget.isVisible();
        boolean secondVisible = secondWidget != null && secondWidget.isVisible();
        if(!(firstVisible && secondVisible))
            return;

        double firstFlex = ((FlexPanel.LayoutData) firstWidget.getLayoutData()).flex / flexSum;
        int realSize = vertical ? firstWidget.getElement().getOffsetHeight() : firstWidget.getElement().getOffsetWidth();
        LocationData newLocation = new LocationData(availableSize, realSize, firstFlex);

        boolean foundBasis = false;
        double fb = 0.0;
        double w1 = newLocation.availableSize, rs1 = newLocation.size, f1 = newLocation.flex;
//         вычисляем базис и регистрируем текущий location
        for(int i=prevLocations.size()-1;i>=0;i--) {
            LocationData prevLocation = prevLocations.get(i);
            // 2 - prev , 1 - new ((w2*f2-rs2)*f1-(w1*f1-rs1)*f2)/(f2-f1)
            double f2 = prevLocation.flex;
            if(Math.abs(f2 - f1) >=0.0001) {
                if(!foundBasis) {
                    double w2 = prevLocation.availableSize, rs2 = prevLocation.size;
                    fb = ((w2*f2-rs2)*f1-(w1*f1-rs1)*f2)/(f2-f1);
                    foundBasis = true;
                }
            } else // обновляем location последним
                prevLocations.remove(i);
        }
        prevLocations.add(newLocation);

        if(prevLocations.size() > 9)
            prevLocations = prevLocations.subList(0, 6);

        rs1 = Math.max(0, availableSize * ratio);

        boolean foundNewFlex1 = false;
        if (foundBasis) {
            for(int i=prevLocations.size()-1;i>=0;i--) {
                LocationData prevLocation = prevLocations.get(i);
                // 2 - prev , 1 - new (rs1-fb)*f2/(rs2-fb+(w1-w2)*f2)
                double w2 = prevLocation.availableSize, rs2 = prevLocation.size, f2 = prevLocation.flex;
                double diffLocation = rs2-fb+(w1-w2)*f2;
                if(Math.abs(diffLocation) >= 5) { // находим location не совпадающий с базисом
                    f1 = (rs1-fb)*f2 / diffLocation;
                    foundNewFlex1 = true;
                    break;
                }
            }
        }
        if(!foundNewFlex1) {
//             считаем что половина идет на базис половина на flex
            fb = Math.max(0, realSize - availableSize * newLocation.flex / 2);
            f1 = (rs1 - fb) * 2 / availableSize;
        }

        f1 = Math.max(0, Math.min(f1, 1));
        double neededFlex2 = 1.0 - f1;
//        int sizeToSet1 = Math.max(0, neededSize - firstChild.getMargins(vertical));
        panel.setChildFlex(firstWidget, f1 * flexSum);
//        int sizeToSet2 = Math.max(0, availableSize - neededSize - secondChild.getMargins(vertical));
        panel.setChildFlex(secondWidget, neededFlex2 * flexSum);

        if(!recheck) {
            int newSize = vertical ? firstWidget.getElement().getOffsetHeight() : firstWidget.getElement().getOffsetWidth();
            if (Math.abs(newSize - rs1) >= 5) // если не попали на 5 пикселей - повторяем
                setSplitSize(ratio, flexSum, true);
        }
    }

    private static class Panel extends FlexPanel {
        FlexSplitPanel splitPanel;
        
        public Panel(boolean vertical) {
            super(vertical);
        }

        private void setSplitPanel(FlexSplitPanel splitPanel) {
            this.splitPanel = splitPanel;
        }

        @Override
        public void onResize() {
            splitPanel.update();
            super.onResize();
        }
    }
}
