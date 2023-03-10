package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.CaptionPanelHeader;

import java.io.Serializable;

public interface BaseImage extends Serializable {

    String TEXT = "lsf-text-caption";
    String DIV = "lsf-div-caption";
    String IMAGE = "lsf-image-caption";

    String getImageElementSrc(boolean enabled);

    default boolean useIcon() {
        return false;
    }

    default Element createImage() {
        Element imageElement;
        if(useIcon()) {
            imageElement = Document.get().createElement("i");
        } else {
            imageElement = Document.get().createImageElement();
        }

        updateImageSrc(imageElement);

        return imageElement;
    }

    default boolean updateImageSrc(Element element) {
        boolean useIcon = !ImageElement.is(element);
        boolean needIcon = useIcon();
        if(useIcon != needIcon)
            return false;

        if (useIcon)
            updateClasses(element, ((BaseStaticImage) this).getFontClasses());
        else
            ((ImageElement) element).setSrc(getImageElementSrc(true));

        return true;
    }

    static void updateClasses(Element element, String classes) {
        String prevClasses = element.getPropertyString(GwtClientUtils.LSF_CLASSES_ATTRIBUTE);
        if(prevClasses != null)
            GwtClientUtils.removeClassNames(element, prevClasses);

        setClasses(element, classes);
    }

    static void setClasses(Element element, String setClasses) {
        if(setClasses != null && !setClasses.isEmpty())
            GwtClientUtils.addClassNames(element, setClasses);

        element.setPropertyString(GwtClientUtils.LSF_CLASSES_ATTRIBUTE, setClasses);
    }

    static void initImageText(Widget widget, String caption, BaseImage appImage, boolean vertical) {
        Element element = widget.getElement();
        // others image texts handle color themes changes with the explicit colorThemeChanged (rerendering the whole view)
        element.addClassName("img-text-widget");
        initImageText(element);
        updateText(widget, caption, vertical);
        updateImage(appImage, widget, vertical);
    }

    static void initImageText(Element element) {
        element.setInnerText("..."); // need this to make getLastChild work
        Node node = element.getLastChild();
        element.setPropertyObject(TEXT, node);
        node.setNodeValue(""); // to remove "..."
    }

    static void clearImageText(Element element, boolean vertical) {
        element.setPropertyObject(TEXT, null);
        element.setPropertyObject(DIV, null);
        element.setPropertyObject(IMAGE, null);

        element.removeClassName("wrap-text-not-empty");
        if(vertical)
            element.removeClassName("wrap-img-vert");
        else
            element.removeClassName("wrap-img-horz");
    }

    static void updateText(Widget widget, String text, boolean vertical) {
        updateText(widget, text, vertical, false);
    }
    static void updateText(Widget widget, String text, boolean vertical, boolean forceDiv) {
        updateText(widget.getElement(), text, vertical, forceDiv);

        if(widget instanceof CaptionPanelHeader)
            widget.setVisible(text != null && !text.isEmpty());
    }
    static void updateText(Element element, String text, boolean vertical) {
        updateText(element, text, vertical, false);
    }
    static void updateText(Element element, String text, boolean vertical, boolean forceDiv) {
        Node textNode = (Node) element.getPropertyObject(TEXT); // always present, since it is initialized in initImageText
        Element htmlElement = (Element) element.getPropertyObject(DIV);

        text = text == null ? "" : text;
        if ((forceDiv && !text.isEmpty()) || EscapeUtils.isContainHtmlTag(text)) {
            if(htmlElement == null) {
                htmlElement = Document.get().createDivElement();
                htmlElement.addClassName("wrap-text-div");

                if(vertical) {
                    element.addClassName("wrap-div-vert");
                } else {
                    element.addClassName("wrap-div-horz");
                }

                element.appendChild(htmlElement);
                element.setPropertyObject(DIV, htmlElement);
            }
            htmlElement.setInnerHTML(text);
            textNode.setNodeValue("");
        } else {
            if(htmlElement != null) {
                element.removeChild(htmlElement);
                if(vertical)
                    element.removeClassName("wrap-div-vert");
                else
                    element.removeClassName("wrap-div-horz");

                element.setPropertyObject(DIV, null);
            }
            textNode.setNodeValue(text);
        }

        if (!text.isEmpty()) {
            element.addClassName("wrap-text-not-empty");
        } else {
            element.removeClassName("wrap-text-not-empty");
        }
    }

    String IMAGE_WIDGET = "lsf-image-widget-caption";

    // updating element with text
    static void updateImage(BaseImage image, Widget widget, boolean vertical) {
        Element element = widget.getElement();
        element.setPropertyObject(IMAGE_WIDGET, new Pair<>(image, vertical));
        updateImage(image, element, vertical);
    }
    static void updateImage(BaseImage image, Element element, boolean vertical) {
        Element imageElement = (Element) element.getPropertyObject(IMAGE);
        if(imageElement != null && (image == null || !image.updateImageSrc(imageElement))) {
            element.removeChild(imageElement); // dropping image to create one after
            if(vertical)
                element.removeClassName("wrap-img-vert");
            else
                element.removeClassName("wrap-img-horz");

            imageElement = null;
        }
        if(imageElement == null) {
            if(image != null) {
                imageElement = image.createImage();
                imageElement.addClassName("wrap-text-img");

                if(vertical) {
                    element.addClassName("wrap-img-vert");
                } else {
                    element.addClassName("wrap-img-horz");
                }

                element.insertFirst(imageElement);
            }
            element.setPropertyObject(IMAGE, imageElement);
        }
    }
}
