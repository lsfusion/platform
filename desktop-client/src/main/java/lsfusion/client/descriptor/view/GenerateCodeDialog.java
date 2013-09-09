package lsfusion.client.descriptor.view;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.FontInfo;
import lsfusion.interop.form.layout.DoNotIntersectSimplexConstraint;
import lsfusion.interop.form.layout.SimplexConstraints;
import lsfusion.interop.form.layout.SingleSimplexConstraint;

import javax.swing.*;
import java.awt.*;

public class GenerateCodeDialog extends JDialog {
    private final FormDescriptor form;

    public GenerateCodeDialog(FormDescriptor iForm) {
        super(null, ModalityType.MODELESS);

        form = iForm;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());

        String design = String.format("DESIGN %s {\n%s", form.getSID(), getTitle(form, 1));

        design += getChildrenDesign(form.client.mainContainer, 1) + "}";

        JTextArea codeTextArea = new JTextArea(design);
        codeTextArea.setFont(new Font("Dialog", Font.BOLD, 15));
        JScrollPane pane = new JScrollPane(codeTextArea);
        add(pane, BorderLayout.CENTER);

    }

    private String getChildrenDesign(ClientContainer container, int offset) {
        String design = "";
        for (ClientComponent child : container.getChildren()) {
            design += getOffset(offset);
            if (child instanceof ClientContainer) {
                design += String.format("NEW %s {\n%s%s%s%s%s%s%s%s%s%s%s%s}\n",
                        child.getSID(),
                        getContainerCaption((ClientContainer) child, offset + 1),
                        getType((ClientContainer) child, offset + 1),
                        getFlex(child, offset + 1),
                        getInsets(child, offset + 1),
                        getDirection(child, offset + 1),
                        getSize(child, offset + 1),
                        getChildrenDesign((ClientContainer) child, offset + 1),
                        getFont(child, offset + 1),
                        getHeaderFont(child, offset + 1),
                        getBackground(child, offset + 1),
                        getForeground(child, offset + 1),
                        getOffset(offset));
            } else if (child instanceof ClientPropertyDraw) {
                String propertyDrawDesign = getCaption(child, offset + 1) +
                        getImage((ClientPropertyDraw) child, offset + 1) +
                        getEditKey((ClientPropertyDraw) child, offset + 1) +
                        getFlex(child, offset + 1) +
                        getInsets(child, offset + 1) +
                        getDirection(child, offset + 1) +
                        getFont(child, offset + 1) +
                        getHeaderFont(child, offset + 1) +
                        getBackground(child, offset + 1) +
                        getForeground(child, offset + 1) +
                        getSize(child, offset + 1);
                design += String.format("ADD PROPERTY(%s)", child.getSID()) +
                        (propertyDrawDesign.isEmpty() ? ";\n" : String.format(" {\n%s%s}\n", propertyDrawDesign, getOffset(offset)));
            } else
                design += String.format("ADD %s;\n", child.getSID());
        }
        return design;
    }

    private String getOffset(int offset) {
        String result = "";
        for (int i = 0; i < offset; i++)
            result += "\t";
        return result;
    }

    private String getSize(ClientComponent component, int offset) {
        String result = "";

        Dimension minSize = component.minimumSize;
        if (minSize != null)
            result += String.format("%sminimumSize = ( %d, %d );\n", getOffset(offset), minSize.width, minSize.height);
        Dimension maxSize = component.maximumSize;
        if (maxSize != null)
            result += String.format("%smaximumSize = ( %d, %d );\n", getOffset(offset), maxSize.width, maxSize.height);
        Dimension prefSize = component.preferredSize;
        if (prefSize != null)
            result += String.format("%spreferredSize = ( %d, %d );\n", getOffset(offset), prefSize.width, prefSize.height);

        if (component instanceof ClientPropertyDraw) {
            int minCharWidth = ((ClientPropertyDraw) component).minimumCharWidth;
            if (minCharWidth != 0)
                result += String.format("%sminimumCharWidth = %d;\n", getOffset(offset), minCharWidth);
            int maxCharWidth = ((ClientPropertyDraw) component).maximumCharWidth;
            if (maxCharWidth != 0)
                result += String.format("%smaximumCharWidth = %d;\n", getOffset(offset), maxCharWidth);
            int prefCharWidth = ((ClientPropertyDraw) component).preferredCharWidth;
            if (prefCharWidth != 0)
                result += String.format("%spreferredCharWidth = %d;\n", getOffset(offset), prefCharWidth);
        }
        return result;
    }

    private String getCaption(ClientComponent component, int offset) {
        String caption = component.getCaption();
        if (caption == null)
            caption = ((ClientPropertyDraw) component).getDescriptor().getPropertyObject().property.caption;
        return caption == null ? "" : String.format("%scaption = '%s';\n", getOffset(offset), caption);
    }

    private String getContainerCaption(ClientContainer container, int offset) {
        String caption = container.getRawCaption();
        return caption == null ? "" : String.format("%scaption = '%s';\n", getOffset(offset), caption);
    }

    private String getTitle(FormDescriptor form, int offset) {
        String title = form.getCaption();
        return title == null ? "" : String.format("%stitle = '%s';\n", getOffset(offset), title);
    }

    private String getImage(ClientPropertyDraw component, int offset) {
        String result = "";
        if (component.design.iconPath != null) {
            result += String.format("%siconPath = '%s';\n", getOffset(offset), component.design.iconPath);
        }
        return result;
    }

    private String getEditKey(ClientPropertyDraw component, int offset) {
        KeyStroke editKey = component.getEditKey();
        boolean showEditKey = component.showEditKey;
        return editKey == null ? "" : String.format("%seditKey = '%s';\n%sshowEditKey = %s;\n", getOffset(offset), String.valueOf(editKey),
                getOffset(offset), String.valueOf(showEditKey).toUpperCase());
    }

    private String getFlex(ClientComponent component, int offset) {
        String result = "";
        double flex = component.getFlex();
        result += getOffset(offset) + "flex = " + flex + ";\n";
        return result;
    }

    private String getInsets(ClientComponent component, int offset) {
        String result = "";
        String insetsInsideTop = component.getConstraints().getInsetsInsideTop();
        String insetsInsideLeft = component.getConstraints().getInsetsInsideLeft();
        String insetsInsideBottom = component.getConstraints().getInsetsInsideBottom();
        String insetsInsideRight = component.getConstraints().getInsetsInsideRight();
        String insetsSiblingTop = component.getConstraints().getInsetsSiblingTop();
        String insetsSiblingLeft = component.getConstraints().getInsetsSiblingLeft();
        String insetsSiblingBottom = component.getConstraints().getInsetsSiblingBottom();
        String insetsSiblingRight = component.getConstraints().getInsetsSiblingRight();
        if (!SimplexConstraints.DEFAULT_INSETS_SIBLING.equals(component.getConstraints().insetsSibling))
            result += getOffset(offset) + String.format("insetsSibling = (%s, %s, %s, %s);\n", insetsSiblingTop, insetsSiblingLeft, insetsSiblingBottom, insetsSiblingRight);

        return result;
    }

    private String getDirection(ClientComponent component, int offset) {
        String directionTop = component.getConstraints().getDirectionsTop();
        String directionLeft = component.getConstraints().getDirectionsLeft();
        String directionBottom = component.getConstraints().getDirectionsBottom();
        String directionRight = component.getConstraints().getDirectionsRight();
        if (!SimplexConstraints.DEFAULT_DIRECTIONS.equals(component.getConstraints().directions))
            return getOffset(offset) + String.format("directions = (%s, %s, %s, %s);\n", directionTop, directionLeft, directionBottom, directionRight);
        else return "";
    }

    private String getFont(ClientComponent component, int offset) {
        FontInfo font = component.design.getFont();
        return font == null ? "" : String.format("%sfont = '\"%s\" %s %s %d';\n", getOffset(offset),
                                                 font.getFontFamily(),
                                                 font.isBold() ? "bold" : "",
                                                 font.isItalic() ? "italic" : "",
                                                 font.getFontSize()
        );
    }

    private String getHeaderFont(ClientComponent component, int offset) {
        FontInfo font = component.design.getHeaderFont();
        return font == null ? "" : String.format("%sheaderFont = '\"%s\" %s %s %d';\n", getOffset(offset),
                                                 font.getFontFamily(),
                                                 font.isBold() ? "bold" : "",
                                                 font.isItalic() ? "italic" : "",
                                                 font.getFontSize()
        );
    }

    private String getBackground(ClientComponent component, int offset) {
        Color bgr = component.design.getBackground();
        return bgr == null ? "" : String.format("%sbackground = %s;\n", getOffset(offset),
                ("#" + toHexString(bgr.getRed(), 2) + toHexString(bgr.getGreen(), 2) + toHexString(bgr.getBlue(),2)));
    }

    private String getForeground(ClientComponent component, int offset) {
        Color fgr = component.design.getForeground();
        return fgr == null ? "" : String.format("%sforeground = %s;\n", getOffset(offset),
                ("#" + toHexString(fgr.getRed(), 2) + toHexString(fgr.getGreen(), 2) + toHexString(fgr.getBlue(),2)));
    }

    private String toHexString(int value, Integer length) {
        String result = Integer.toHexString(value);
        while(result.length()<length)
            result = "0" + value;
        return result;
    }


    private String getType(ClientContainer container, int offset) {
        String type;
        DoNotIntersectSimplexConstraint constraints = container.getConstraints().childConstraints;
        if (constraints.equals(SingleSimplexConstraint.TOTHE_BOTTOM))
            type = "CONTAINERV";
        else if (constraints.equals(SingleSimplexConstraint.TOTHE_RIGHT))
            type = "CONTAINERH";
        else if (constraints.equals(SingleSimplexConstraint.TOTHE_RIGHTBOTTOM))
            type = "COLUMNS";
        else {
            switch (container.getType()) {
                case CONTAINERH:
                    type = "CONTAINERH";
                    break;
                case CONTAINERV:
                    type = "CONTAINERV";
                    break;
                case COLUMNS:
                    type = "COLUMNS";
                    break;
                case TABBED_PANE:
                    type = "TABBED";
                    break;
                case VERTICAL_SPLIT_PANE:
                    type = "SPLITV";
                    break;
                case HORIZONTAL_SPLIT_PANE:
                    type = "SPLITH";
                    break;
                case SCROLL:
                    throw new IllegalStateException("SCROLL container isn't yet supported");
                case FLOW:
                    throw new IllegalStateException("FLOW container isn't yet supported");
                default:
                    throw new IllegalStateException("shouldn't happen");
            }
        }
        return getOffset(offset) + "type = " + type + ";\n";
    }
}
