package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.view.FlexPanel;

import java.io.Serializable;

public interface BaseImage extends Serializable {

    String TEXT = "lsf-text-caption";
    String TYPE = "lsf-image-text-type";
    String IMAGE_VERTICAL = "lsf-image-text-vertical";
    String DIV = "lsf-div-caption";
    String IMAGE = "lsf-image-caption";

    String BASE_STATIC_IMAGE = "lsf-base-static-image";

    String getImageElementSrc(boolean enabled);

    default boolean useIcon() {
        return false;
    }

    default String createImageHTML() {
        if(useIcon())
            return "<i class=\"" + ((BaseStaticImage) this).getFontClasses() + " wrap-text-img\"></i>";
        else
            return "<img src=\"" + getImageElementSrc(true) + "\" class=\"wrap-text-img\"></img>";
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

    static void updateClasses(Widget widget, String classes) {
        updateClasses(widget.getElement(), classes);
    }

    interface PropagateClasses {
        boolean is(Widget widget, Widget parent, String className);
    }
    static void updateClasses(Widget widget, String classes, PropagateClasses propagateClasses) {
        updateClasses(widget, buildClassesChanges(widget.getElement(), classes, emptyPostfix), propagateClasses);
    }
    static void updateClasses(Widget widget, NativeStringMap<Object> classChanges, PropagateClasses propagateClasses) {
        Widget parent = widget.getParent();

        NativeStringMap<Object> propagateClassChanges = new NativeStringMap<>();
        classChanges.foreachEntry((aclass, value) -> {
            if(value instanceof Boolean) { // need only classes
                boolean propagated = false;
                boolean add = (boolean) value;
                // in mobile forms window is added directly into RootLayoutPanel
                if (parent instanceof FlexPanel && propagateClasses.is(widget, parent, aclass)) {
                    int prevAggrClasses = parent.getElement().getPropertyInt("PROPAGATE_CLASS_" + aclass);
                    int newAggrClasses = prevAggrClasses + (add ? 1 : -1);
                    parent.getElement().setPropertyInt("PROPAGATE_CLASS_" + aclass, newAggrClasses);

                    int childCount = ((FlexPanel) parent).getWidgetCount(); // parentElement.getChildCount
                    boolean prevNeedOnHover = prevAggrClasses == childCount;
                    boolean newNeedOnHover = newAggrClasses == childCount;
                    if (prevNeedOnHover != newNeedOnHover) { // changed status
                        assert newNeedOnHover == add;
                        propagated = true;
                        for (int i = 0; i < childCount; i++) { // updating siblings
                            Widget siblingWidget = ((FlexPanel) parent).getWidget(i); // parentElement.getChild()
                            if (!widget.equals(siblingWidget))
                                applyClassChange(siblingWidget.getElement(), aclass, !add);
                        }
                    }
                }
                if (propagated) {
                    propagateClassChanges.put(aclass, add);
                } else {
                    applyClassChange(widget.getElement(), aclass, add);
                }
            } else
                applyClassChange(widget.getElement(), aclass, value);
        });

        if(!propagateClassChanges.isEmpty()) { // optimization
            // parents has classes only set with propagations (in this case we don't need to update LSF_CLASSES_ATTRIBUTE)
            assert parent.getElement().getPropertyObject(GwtClientUtils.LSF_CLASSES_ATTRIBUTE) == null;
            updateClasses(parent, propagateClassChanges, propagateClasses);
        }
    }

    String emptyPostfix = "";

    //use postfix to avoid intersection valueElementClass with GComponent.elementClass
    static NativeStringMap<Object> buildClassesChanges(Element element, String newClasses, String postfix) {
        String[] prevClasses = (String[]) element.getPropertyObject(GwtClientUtils.LSF_CLASSES_ATTRIBUTE + postfix);
        if(prevClasses == null)
            prevClasses = new String[0];

        NativeStringMap<Object> changes = new NativeStringMap<>();
        for(String prevClass : prevClasses) {
            String[] values = GwtClientUtils.splitUnquotedEqual(prevClass);
            changes.put(values[0], values.length > 1 || prevClass.endsWith("=") ? null : false);
        }

        String[] classes = newClasses != null ? GwtClientUtils.splitUnquotedSpace(newClasses) : new String[0];
        for (String newClass : classes) {
            String[] values = GwtClientUtils.splitUnquotedEqual(newClass);
            if (changes.remove(values[0]) == null)
                changes.put(values[0], values.length > 1 ? GwtClientUtils.unquote(values[1]) : (newClass.endsWith("=") ? "" : true));
        }
        element.setPropertyObject(GwtClientUtils.LSF_CLASSES_ATTRIBUTE + postfix, classes);

        return changes;
    }

    static void applyClassChange(Element element, String aclass, Object value) {
        if (!GwtSharedUtils.isRedundantString(aclass)) {
            if(value instanceof Boolean) { //class
                if ((boolean) value)
                    element.addClassName(aclass);
                else
                    element.removeClassName(aclass);

            } else { //attr or style
                if (value != null)
                    GwtClientUtils.setAttributeOrStyle(element, aclass, ((String)value));
//                    element.setAttribute(aclass, (String) value);
                else
                    GwtClientUtils.removeAttributeOrStyle(element, aclass);
//                    element.removeAttribute(aclass);
            }
        }
    }

    static void updateClasses(Element element, String classes) {
        updateClasses(element, classes, emptyPostfix);
    }
    static void updateClasses(Element element, String classes, String postfix) {
        buildClassesChanges(element, classes, postfix).foreachEntry((aclass, value) -> {
            applyClassChange(element, aclass, value);
        });
    }

//    static boolean hasClassPrefix(Element element, String classPrefix) {
//        // we're using lsf set (and not all) css  classes, to avoid unpredictable resulta
//        String[] prevClasses = (String[]) element.getPropertyObject(GwtClientUtils.LSF_CLASSES_ATTRIBUTE);
//        if(prevClasses != null) {
//            for(String prevClass : prevClasses) {
//                if(prevClass.startsWith(classPrefix))
//                    return true;
//            }
//        }
//        return false;
//    }

    static void initImageText(Widget widget, String caption, BaseImage appImage, ImageHtmlOrTextType type) {
        Element element = widget.getElement();
        // others image texts handle color themes changes with the explicit colorThemeChanged (rerendering the whole view)
        element.addClassName("img-text-widget");
        initImageText(element, type);
        updateText(widget, caption);
        updateImage(appImage, widget);
    }

    static void initImageText(Element element, ImageHtmlOrTextType type) {
        element.setInnerText("..."); // need this to make getLastChild work

        GwtClientUtils.initCaptionHtmlOrText(element, type); // actually only text can be here (because of containHTMLTag check)
        element.setPropertyObject(TYPE, type);

        element.setPropertyBoolean(IMAGE_VERTICAL, type.isImageVertical());

        Node node = element.getLastChild();
        element.setPropertyObject(TEXT, node);
        node.setNodeValue(""); // to remove "..."
    }

    static void clearImageText(Element element) {
        element.setPropertyObject(TEXT, null);
        element.setPropertyObject(DIV, null);
        element.setPropertyObject(IMAGE, null);

        element.setPropertyObject(BASE_STATIC_IMAGE, null);

        element.setPropertyObject(TYPE, null);

        element.removeClassName("wrap-text-not-empty");
        if(element.getPropertyBoolean(IMAGE_VERTICAL))
            element.removeClassName("wrap-img-vert");
        else
            element.removeClassName("wrap-img-horz");
        element.removeClassName("wrap-img-start");

        element.setPropertyObject(IMAGE_VERTICAL, null);
    }

    static void updateText(Widget widget, String text) {
        updateText(widget, text, false);
    }
    static void updateText(Widget widget, String text, boolean forceDiv) {
        updateText(widget.getElement(), text, forceDiv);
    }
    static void updateText(Element element, String text) {
        updateText(element, text, false);
    }
    static void updateText(Element element, String text, boolean forceDiv) {
        Node textNode = (Node) element.getPropertyObject(TEXT); // always present, since it is initialized in initImageText
        Element textElement = (Element) element.getPropertyObject(DIV);

        // using node for optimization purposes (to save extra element in DOM)
        text = text == null ? "" : text;
        if ((forceDiv && !text.isEmpty()) || GwtClientUtils.containsHtmlTag(text)) { // nodeValue doesn't support line breaks
            if(textElement == null) {
                textElement = Document.get().createDivElement();
                ImageHtmlOrTextType type = (ImageHtmlOrTextType) element.getPropertyObject(TYPE);
                GwtClientUtils.initCaptionHtmlOrText(textElement, type);
                textElement.addClassName("wrap-text-div");

                if(element.getPropertyBoolean(IMAGE_VERTICAL)) {
                    element.addClassName("wrap-div-vert");
                } else {
                    element.addClassName("wrap-div-horz");
                }

                element.appendChild(textElement);
                element.setPropertyObject(DIV, textElement);
            }
            GwtClientUtils.setCaptionHtmlOrText(textElement, text);
            textNode.setNodeValue("");
        } else {
            if(textElement != null) {
                element.removeChild(textElement);
                if(element.getPropertyBoolean(IMAGE_VERTICAL))
                    element.removeClassName("wrap-div-vert");
                else
                    element.removeClassName("wrap-div-horz");

                element.setPropertyObject(DIV, null);
            }
            GwtClientUtils.setCaptionNodeText(textNode, text);
        }

        if (!text.isEmpty()) {
            element.addClassName("wrap-text-not-empty");
        } else {
            element.removeClassName("wrap-text-not-empty");
        }
    }

    // updating element with text
    static void updateImage(BaseImage image, Widget widget) {
        Element element = widget.getElement();
        updateImage(image, element);
    }
    static void updateImage(BaseImage image, Element element) {
        element.setPropertyObject(BASE_STATIC_IMAGE, image instanceof BaseStaticImage ? image : null);

        Element imageElement = (Element) element.getPropertyObject(IMAGE);
        if(imageElement != null && (image == null || !image.updateImageSrc(imageElement))) {
            element.removeChild(imageElement); // dropping image to create one after
            if(element.getPropertyBoolean(IMAGE_VERTICAL))
                element.removeClassName("wrap-img-vert");
            else
                element.removeClassName("wrap-img-horz");
            element.removeClassName("wrap-img-start");

            imageElement = null;
        }
        if(imageElement == null) {
            if(image != null) {
                imageElement = image.createImage();
                imageElement.addClassName("wrap-text-img");

                if(element.getPropertyBoolean(IMAGE_VERTICAL)) {
                    element.addClassName("wrap-img-vert");
                } else {
                    element.addClassName("wrap-img-horz");
                }
                element.addClassName("wrap-img-start");

                element.insertFirst(imageElement);
            }
            element.setPropertyObject(IMAGE, imageElement);
        }
    }
}
