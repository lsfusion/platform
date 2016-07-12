package lsfusion.interop.form.layout;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.interop.form.layout.Alignment.*;

public class FlexLayout extends CachableLayout<FlexConstraints> {

    private final boolean vertical;
    private final Alignment alignment;

    protected final Map<Component, FlexConstraints> constraintsMap;

    public FlexLayout(Container target, boolean vertical) {
        this(target, vertical, CENTER);
    }

    public FlexLayout(Container target, boolean vertical, Alignment alignment) {
        super(target);
        this.vertical = vertical;
        this.alignment = alignment;

        this.constraintsMap = new HashMap<Component, FlexConstraints>();
    }

    @Override
    protected FlexConstraints getDefaultContraints() {
        return new FlexConstraints();
    }

    @Override
    protected FlexConstraints cloneConstraints(FlexConstraints original) {
        return (FlexConstraints) original.clone();
    }

    @Override
    protected Dimension layoutSize(Container parent, ComponentSizeGetter sizeGetter) {
        int width = 0;
        int height = 0;
        int childCnt = target.getComponentCount();
        for (int i = 0; i < childCnt; ++i) {
            Component child = target.getComponent(i);
            if (child.isVisible()) {
                Dimension childSize = sizeGetter.get(child);
                int childWidth = childSize.width;
                int childHeight = childSize.height;

                if (vertical) {
                    width = Math.max(width, childWidth);
                    height = limitedSum(height, childHeight);
                } else {
                    width = limitedSum(width, childWidth);
                    height = Math.max(height, childHeight);
                }
            }
        }
        return new Dimension(width, height);
    }

    @Override
    public void layoutContainer(Container parent) {
        checkParent(parent);

        Dimension size = target.getSize();
        Insets in = target.getInsets();

        int parentWidth = size.width - in.left - in.right;
        int parentHeight = size.height - in.top - in.bottom;

        int childCnt = target.getComponentCount();
        int totalSize = 0;
        double totalFlex = 0;
        for (int i = 0; i < childCnt; ++i) {
            Component child = target.getComponent(i);
            if (child.isVisible()) {
                Dimension prefSize = child.getPreferredSize();
                FlexConstraints constraints = lookupConstraints(child);

                totalFlex += constraints.getFlex();
                if (constraints.getFlex() == 0) {
                    totalSize += vertical ? prefSize.height : prefSize.width;
                }
            }
        }

        int fillSpace = Math.max(0, vertical ? parentHeight - totalSize : parentWidth - totalSize);

        //All alignment
        if (totalFlex == 0 && alignment != LEADING && fillSpace > 0) {
            int alignmentOffset = alignment == CENTER ? fillSpace / 2 : fillSpace;
            if (vertical) {
                in.top += alignmentOffset;
            } else {
                in.left += alignmentOffset;
            }
        }

        int xOffset = in.left;
        int yOffset = in.top;
        for (int i = 0; i < childCnt; ++i) {
            Component child = target.getComponent(i);
            if (child.isVisible()) {
                Dimension prefSize = child.getPreferredSize();
                Dimension minSize = child.getMinimumSize();

                int minWidth = minSize.width;
                int minHeight = minSize.height;
                int prefWidth = prefSize.width;
                int prefHeight = prefSize.height;

                FlexConstraints childConstraints = lookupConstraints(child);
                double flex = childConstraints.getFlex();
                FlexAlignment align = childConstraints.getAlignment();

                int width;
                int height;

                if (vertical) {
                    width = limitedSize(align == FlexAlignment.STRETCH, prefWidth, minWidth, parentWidth);
                    height = flex == 0 ? prefHeight : (int) (flex * fillSpace / totalFlex);
                    xOffset = getAlignmentOffset(align, in.left, parentWidth, width);
                } else {
                    width = flex == 0 ? prefWidth : (int) (flex * fillSpace / totalFlex);
                    height = limitedSize(align == FlexAlignment.STRETCH, prefHeight, minHeight, parentHeight);
                    yOffset = getAlignmentOffset(align, in.top, parentHeight, height);
                }

                Rectangle bounds = child.getBounds();
                if (bounds.x != xOffset || bounds.y != yOffset || bounds.width != width || bounds.height != height) {
                    child.setBounds(xOffset, yOffset, width, height);
                }

                if (vertical) {
                    yOffset += height;
                } else {
                    xOffset += width;
                }
            }
        }
    }

    private int limitedSize(boolean stretch, int pref, int min, int parent) {
        if (stretch) {
            return parent;
        }

        if (pref <= parent) {
            return pref;
        }

        if (min >= parent) {
            return min;
        }

        return parent;
    }

    private int getAlignmentOffset(FlexAlignment alignment, int zeroOffset, int totalSize, int componentSize) {
        switch (alignment) {
            case LEADING: return zeroOffset;
            case CENTER: return zeroOffset + Math.max(0, (totalSize - componentSize)/2);
            case TRAILING: return zeroOffset + Math.max(0, totalSize - componentSize);
            case STRETCH: return zeroOffset;
            default :
                throw new IllegalStateException("Wrong alignment value");
        }
    }
}
