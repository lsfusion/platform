package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

import java.util.function.BiConsumer;

public class NavigatorImageButton extends ImageButton {

    private GNavigatorElement element;

    public NavigatorImageButton(GNavigatorElement element, boolean vertical, int level, boolean active, BiConsumer<GNavigatorElement, NativeEvent> clickHandler) {
        this(element, vertical, false, level, active);

        addClickHandler(event -> clickHandler.accept(element, event.getNativeEvent()));
    }
    public NavigatorImageButton(GNavigatorElement element, boolean vertical, boolean span, int level, boolean active) {
        super(element.caption, element.image, vertical, span ? Document.get().createSpanElement() : Document.get().createAnchorElement());

        addStyleName("nav-item nav-link navbar-text");
        addStyleName(vertical ? "nav-link-vert" : "nav-link-horz");
        this.vertical = vertical;

        this.element = element;
        this.level = level;
        this.active = active;

        update();
    }

    private void update() {
        addStyleName((vertical ? "nav-link-vert" : "nav-link-horz") + "-" + level);
        if(active)
            addStyleName("active");

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
        };
        TooltipManager.initTooltip(this.getElement(), tooltipHelper);
    }

    private final boolean vertical;

    private int level = -1;
    private boolean active;
    public void change(GNavigatorElement element, int level, boolean active) {
        if(this.active)
            removeStyleName("active");
        if(this.level >= 0)
            removeStyleName((vertical ? "nav-link-vert" : "nav-link-horz") + "-" + this.level);

        this.element = element;
        this.level = level;
        this.active = active;

        updateImage();
        updateText();
        update();
    }
    public void updateElementClass() {
        BaseImage.updateClasses(this, element.elementClass);

        updateForceDiv(); // forceDiv might change
    }

    @Override
    protected BaseImage getImage() {
        return element.image;
    }

    private static boolean isForceDiv(String elementClass) {
        return elementClass != null && (elementClass.contains(WindowsController.NAVBAR_TEXT_ON_HOVER) || elementClass.contains(WindowsController.NAVBAR_TEXT_HIDDEN));
    }

    private boolean forceDiv;
    private void updateForceDiv() {
        String elementClass = element.elementClass;
        GNavigatorWindow drawWindow = element.getDrawWindow();
        boolean newForceDiv = isForceDiv(elementClass) || drawWindow != null && isForceDiv(drawWindow.elementClass);
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
