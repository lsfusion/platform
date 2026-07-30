package lsfusion.gwt.client.navigator.view;

import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.LinkedHashSet;

// the standard panel: one button per drawn element, in one strip laid out by the window's own options
public class ToolbarNavigatorView extends GAbstractNavigatorView {

    private final GINavigatorController navigatorController;

    private final ResizableComplexPanel panel;
    private final boolean verticalTextAlign;

    public ToolbarNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController) {
        this(window, navigatorController, new ToolbarPanel(window.isVertical(), window));
    }

    private ToolbarNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController, ToolbarPanel main) {
        super(window, main);

        this.navigatorController = navigatorController;
        this.verticalTextAlign = window.hasVerticalTextPosition();
        this.panel = main.panel;
    }

    @Override
    public void refresh(LinkedHashSet<GNavigatorElement> elements, GNavigatorElement selected) {
        int[] index = {0}; // the button already at this position is re-pointed at the element instead of being rebuilt
        forEachDrawn(window, elements, (element, level) -> {
            boolean active = window.isActive(element, selected);
            if (index[0] < panel.getWidgetCount())
                ((NavigatorImageButton) panel.getWidget(index[0])).change(element, level, active);
            else
                panel.add(new NavigatorImageButton(element, verticalTextAlign, level, active, navigatorController::activate));
            index[0]++;
        });

        for (int i = index[0], size = panel.getWidgetCount(); i < size; i++)
            panel.remove(index[0]);
    }
}
