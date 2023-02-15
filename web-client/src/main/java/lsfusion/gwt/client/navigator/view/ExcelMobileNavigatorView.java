package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import java.util.HashMap;
import java.util.Map;

public class ExcelMobileNavigatorView implements MobileNavigatorView, ColorThemeChangeListener {
    private GINavigatorController navigatorController;
    private Map<GNavigatorElement, Element> icons = new HashMap<>();

    public ExcelMobileNavigatorView(GNavigatorElement root, GINavigatorController navigatorController) {
        this.navigatorController = navigatorController;

        Element menuElement = createNavigatorMenu(root);
        RootLayoutPanel.get().getElement().appendChild(menuElement);
        enableMMenu(menuElement, ClientMessages.Instance.get().navigator());

        MainFrame.addColorThemeChangeListener(this);
    }

    private Element createNavigatorMenu(GNavigatorElement root) {
        Element navElement = Document.get().createElement("nav");
        navElement.setId("lsfNavigatorMenu");

        UListElement menuULElement = Document.get().createULElement();
        navElement.appendChild(menuULElement);

        for (GNavigatorElement child : root.children) {
            menuULElement.appendChild(createMenuItem(child));
        }
        return navElement;
    }

    private Element createMenuItem(GNavigatorElement navigatorElement) {
        LIElement liElement = Document.get().createLIElement();

        Element iconImageElement = null;
        if (navigatorElement.appImage != null) {
            iconImageElement = navigatorElement.appImage.createImage();
            icons.put(navigatorElement, iconImageElement);
        }

        Element textElement;
        UListElement subMenuElement = null;

        if (navigatorElement.children.size() > 0) {
            textElement = Document.get().createSpanElement();
            
            subMenuElement = Document.get().createULElement();
            for (GNavigatorElement child : navigatorElement.children) {
                subMenuElement.appendChild(createMenuItem(child));
            }
        } else {
            textElement = Document.get().createAnchorElement();
            Event.sinkEvents(textElement, Event.ONCLICK);
            Event.setEventListener(textElement, event -> {
                if (Event.ONCLICK == event.getTypeInt()) {
                    navigatorController.openElement(navigatorElement, event);
                    closeNavigatorMenu();
                }
            });
        }

        if (iconImageElement != null) {
            liElement.appendChild(iconImageElement);
        }
        
        textElement.setInnerText(navigatorElement.caption);
        liElement.appendChild(textElement);

        if (subMenuElement != null) {
            liElement.appendChild(subMenuElement);
        }

        return liElement;
    }

    public final native void enableMMenu(Element element, String title) /*-{
        menu = new $wnd.MmenuLight(element);
        navigator = menu.navigation({
            title: title
        });
        drawer = menu.offcanvas();
    }-*/;

    @Override
    public native void openNavigatorMenu() /*-{
        drawer.open();
    }-*/;

    @Override
    public native void closeNavigatorMenu() /*-{
        drawer.close();
    }-*/;

    @Override
    public void colorThemeChanged() {
        for (Map.Entry<GNavigatorElement, Element> icon : icons.entrySet())
            icon.getKey().appImage.setImageSrc(icon.getValue());
    }
}
