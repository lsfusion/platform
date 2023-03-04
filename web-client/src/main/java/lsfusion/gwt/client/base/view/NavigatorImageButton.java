package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.navigator.GNavigatorElement;

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

        String elementClass = element.elementClass;
        if(elementClass != null)
            addStyleName(elementClass);

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

    @Override
    protected BaseImage getImage() {
        return element.image;
    }

    @Override
    protected String getCaption() {
        return element.caption;
    }
}
