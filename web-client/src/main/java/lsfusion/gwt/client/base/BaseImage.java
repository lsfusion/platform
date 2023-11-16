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
    String DIV = "lsf-div-caption";
    String IMAGE = "lsf-image-caption";

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
        updateClasses(widget, buildClassesChanges(widget.getElement(), classes), propagateClasses);
    }
    static void updateClasses(Widget widget, NativeStringMap<Boolean> classChanges, PropagateClasses propagateClasses) {
        Widget parent = widget.getParent();

        NativeStringMap<Boolean> propagateClassChanges = new NativeStringMap<>();
        classChanges.foreachEntry((aclass, add) -> {
            boolean propagated = false;
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
        });

        if(!propagateClassChanges.isEmpty()) { // optimization
            // parents has classes only set with propagations (in this case we don't need to update LSF_CLASSES_ATTRIBUTE)
            assert parent.getElement().getPropertyObject(GwtClientUtils.LSF_CLASSES_ATTRIBUTE) == null;
            updateClasses(parent, propagateClassChanges, propagateClasses);
        }
    }

    static NativeStringMap<Boolean> buildClassesChanges(Element element, String newClasses) {
        String[] prevClasses = (String[]) element.getPropertyObject(GwtClientUtils.LSF_CLASSES_ATTRIBUTE);
        if(prevClasses == null)
            prevClasses = new String[0];

        NativeStringMap<Boolean> changes = new NativeStringMap<>();
        for(String prevClass : prevClasses) {
            changes.put(prevClass, false);
        }

        String[] classes = newClasses != null ? newClasses.split(" ") : new String[0];
        for(String newClass : classes) {
            if(changes.remove(newClass) == null)
                changes.put(newClass, true);
        }
        element.setPropertyObject(GwtClientUtils.LSF_CLASSES_ATTRIBUTE, classes);

        return changes;
    }

    static void applyClassChange(Element element, String aclass, Boolean add) {
        if (!GwtSharedUtils.isRedundantString(aclass)) {
            if (add)
                element.addClassName(aclass);
            else
                element.removeClassName(aclass);
        }
    }

    static void updateClasses(Element element, String classes) {
        buildClassesChanges(element, classes).foreachEntry((aclass, add) -> {
            applyClassChange(element, aclass, add);
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
        element.removeClassName("wrap-img-start");
    }

    static void updateText(Widget widget, String text, boolean vertical) {
        updateText(widget, text, vertical, false);
    }
    static void updateText(Widget widget, String text, boolean vertical, boolean forceDiv) {
        updateText(widget.getElement(), text, vertical, forceDiv);
    }
    static void updateText(Element element, String text, boolean vertical) {
        updateText(element, text, vertical, false);
    }
    static void updateText(Element element, String text, boolean vertical, boolean forceDiv) {
        Node textNode = (Node) element.getPropertyObject(TEXT); // always present, since it is initialized in initImageText
        Element textElement = (Element) element.getPropertyObject(DIV);

        // using node for optimization purposes (to save extra element in DOM)
        text = text == null ? "" : text;
        if ((forceDiv && !text.isEmpty()) || GwtClientUtils.containsHtmlTag(text) || GwtClientUtils.containsLineBreak(text)) { // nodeValue doesn't support line breaks
            if(textElement == null) {
                textElement = Document.get().createDivElement();
                textElement.addClassName("wrap-text-div");

                if(vertical) {
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
            element.removeClassName("wrap-img-start");

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
                element.addClassName("wrap-img-start");

                element.insertFirst(imageElement);
            }
            element.setPropertyObject(IMAGE, imageElement);
        }
    }
}
