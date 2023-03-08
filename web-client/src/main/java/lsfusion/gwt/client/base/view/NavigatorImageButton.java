package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

public class NavigatorImageButton extends ImageButton {

    private final GNavigatorElement element;

    public NavigatorImageButton(GNavigatorElement element, boolean vertical, int level) {
        this(element, vertical, false, level);
    }
    public NavigatorImageButton(GNavigatorElement element, boolean vertical, boolean span, int level) {
        super(element.caption, element.image, vertical, span ? Document.get().createSpanElement() : Document.get().createAnchorElement());

        this.element = element;

        addStyleName("nav-item nav-link navbar-text");

        addStyleName(vertical ? "nav-link-vert" : "nav-link-horz");
        addStyleName((vertical ? "nav-link-vert" : "nav-link-horz") + "-" + level);

        updateElementClass();

        // debug info
        getElement().setAttribute("lsfusion-container", element.canonicalName);

        TooltipManager.TooltipHelper tooltipHelper = new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return element.getTooltipText();
            }

            @Override
            public String getPath() {
                return element.path;
            }

            @Override
            public String getCreationPath() {
                return element.creationPath;
            }

            @Override
            public boolean stillShowTooltip() {
                return isAttached() && isVisible();
            }
        };
        TooltipManager.registerWidget(this, tooltipHelper);
    }

    public void updateElementClass() {
        BaseImage.updateClasses(getElement(), element.elementClass);

        updateForceDiv(); // forceDiv might change
    }

    @Override
    protected BaseImage getImage() {
        return element.image;
    }

    private boolean forceDiv;
    private void updateForceDiv() {
        String elementClass = element.elementClass;
        GNavigatorWindow drawWindow = element.getDrawWindow();
        boolean newForceDiv = (elementClass != null && elementClass.contains(WindowsController.NAVBAR_TEXT_ON_HOVER)) ||
                                drawWindow != null && drawWindow.forceDiv();
        if(forceDiv != newForceDiv) {
            forceDiv = newForceDiv;
            updateText();
        }
    }

    protected boolean forceDiv() {
        return forceDiv;
    }

    @Override
    protected String getCaption() {
        return element.caption;
    }
}
