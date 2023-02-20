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
    String HTML = "lsf-html-caption";
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

        if (useIcon) {
            String fontClasses = ((BaseStaticImage)this).getFontClasses();
            String prevFontClasses = element.getPropertyString(GwtClientUtils.FONT_CLASSES_ATTRIBUTE);
            if(prevFontClasses != null)
                GwtClientUtils.removeClassNames(element, prevFontClasses);

            // it seems that enabled is not needed, since it is handled with the text color
            GwtClientUtils.addClassNames(element, fontClasses);

            element.setPropertyString(GwtClientUtils.FONT_CLASSES_ATTRIBUTE, fontClasses);
        } else {
            ((ImageElement) element).setSrc(getImageElementSrc(true));
        }
        return true;
    }

    static void initImageText(Widget widget, String caption, BaseImage appImage, boolean vertical) {
        initImageText(widget.getElement());
        updateText(widget, caption);
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
        element.setPropertyObject(HTML, null);
        element.setPropertyObject(IMAGE, null);

        element.removeClassName("wrap-text-not-empty");
        if(vertical)
            element.removeClassName("wrap-img-vert");
        else
            element.removeClassName("wrap-img-horz");
    }

    static void updateText(Widget widget, String text) {
        updateText(widget.getElement(), text);

        if(widget instanceof CaptionPanelHeader)
            widget.setVisible(text != null && !text.isEmpty());
    }
    static void updateText(Element element, String text) {
        Node textNode = (Node) element.getPropertyObject(TEXT); // always present, since it is initialized in initImageText
        Element htmlElement = (Element) element.getPropertyObject(HTML);

        text = text == null ? "" : text;
        if (EscapeUtils.isContainHtmlTag(text)) {
            if(htmlElement == null) {
                htmlElement = Document.get().createSpanElement(); // we want display inline just as regular text
                element.appendChild(htmlElement);
                element.setPropertyObject(HTML, htmlElement);
            }
            htmlElement.setInnerHTML(text);
            textNode.setNodeValue("");
        } else {
            if(htmlElement != null) {
                element.removeChild(htmlElement);
                element.setPropertyObject(HTML, null);
            }
            textNode.setNodeValue(text);
        }

        if (!text.equals("")) {
            element.addClassName("wrap-text-not-empty");
        } else {
            element.removeClassName("wrap-text-not-empty");
        }
    }

    // to remove later
    static void setInnerContent(Element element, String value) {
        value = value == null ? "" : value;
        if (EscapeUtils.isContainHtmlTag(value))
            element.setInnerHTML(value);
        else
            element.setInnerText(value);
    }

    // updating element with text
    static void updateImage(BaseImage image, Widget widget, boolean vertical) {
        updateImage(image, widget.getElement(), vertical);
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
