package lsfusion.interop.base.view;

import lsfusion.base.BaseUtils;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

public class FlexLayout implements LayoutManager2, Serializable {

    private final boolean vertical;
    private final FlexAlignment alignment;

    public FlexLayout(Container target, boolean vertical, FlexAlignment alignment) {
        this.target = target;
        this.vertical = vertical;
        this.alignment = alignment;
    }

    protected final Container target;

    @Override
    public void addLayoutComponent(String name, Component child) {
        throw new IllegalStateException("CachableLayout doesn't use string constraints");
    }

    public void addLayoutComponent(Component child, Object constraints) {
        assert constraints == null;
//        setConstraints(child, (C) constraints);
    }

    @Override
    public void removeLayoutComponent(Component child) {
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(0, 0);
    }

    // we need this because we want to use flexPreferredSize for the same direction flex panel (web browser does that)
    protected Dimension prefFlexSize;
    protected Dimension prefSize;

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        checkParent(parent);
        if (prefSize != null) {
            return prefSize;
        }

        return prefSize = layoutSizeWithInsets(parent, child -> ((FlexComponent)child).getFlexPreferredSize(vertical));
    }

    public Dimension preferredFlexLayoutSize(Container parent) {
        checkParent(parent);
        if (prefFlexSize != null) {
            return prefFlexSize;
        }

        return prefFlexSize = layoutSizeWithInsets(parent, child -> ((FlexComponent)child).getFlexPreferredSize(null));
    }

    protected Dimension layoutSizeWithInsets(Container parent, ComponentSizeGetter sizeGetter) {
        return addInsets(parent, layoutSize(parent, sizeGetter));
    }

    public static Dimension addInsets(Container container, Dimension d) {
        Insets insets = container.getInsets();
        d.width = limitedSum(d.width, insets.left, insets.right);
        d.height = limitedSum(d.height, insets.top, insets.bottom);
        return d;
    }

    protected void checkParent(Container target) {
        assert SwingUtilities.isEventDispatchThread();
        if (this.target != target) {
            throw new AWTError("CachableLayout can't be shared");
        }
    }

    public static int limitedSum(int a, int b) {
        return (int) Math.min((long)a + (long)b, Integer.MAX_VALUE);
    }

    public static int limitedSum(int a, int b, int c) {
        return (int) Math.min((long)a + (long)b + (long)c, Integer.MAX_VALUE);
    }

    public static int limitedSum(int a, int b, int c, int d) {
        return (int) Math.min((long)a + (long)b + (long)c + (long)d, Integer.MAX_VALUE);
    }

    public interface ComponentSizeGetter {
        Dimension get(Component child);
    }

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
    public void invalidateLayout(Container parent) {
        checkParent(parent);
        prefSize = null;
        prefFlexSize = null;
    }

    @Override
    public void layoutContainer(Container parent) {
        checkParent(parent);

//        System.out.println("Layouting " + target);

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
                Dimension prefSize = ((FlexComponent)child).getFlexPreferredSize(null);
                FlexConstraints constraints = ((FlexComponent)child).getFlexConstraints();

                totalFlex += constraints.getFlex();
                totalSize += vertical ? prefSize.height : prefSize.width;
            }
        }

        int fillSpace = Math.max(0, vertical ? parentHeight - totalSize : parentWidth - totalSize);

        //All alignment
        if (totalFlex == 0 && alignment != FlexAlignment.START && fillSpace > 0) {
            int alignmentOffset = alignment == FlexAlignment.CENTER ? fillSpace / 2 : fillSpace;
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
                Dimension prefSize = ((FlexComponent)child).getFlexPreferredSize(null);
                FlexConstraints childConstraints = ((FlexComponent)child).getFlexConstraints();

                int prefWidth = prefSize.width;
                int prefHeight = prefSize.height;

                double flex = childConstraints.getFlex();
                FlexAlignment align = childConstraints.getAlignment();

                int width;
                int height;

                if (vertical) {
                    width = limitedSize(align == FlexAlignment.STRETCH, prefWidth, parentWidth);
                    height = flex == 0 ? prefHeight : prefHeight + (int) (flex * fillSpace / totalFlex);
                    xOffset = getAlignmentOffset(align, in.left, parentWidth, width);
                } else {
                    width = flex == 0 ? prefWidth : prefWidth + (int) (flex * fillSpace / totalFlex);
                    height = limitedSize(align == FlexAlignment.STRETCH, prefHeight, parentHeight);
                    yOffset = getAlignmentOffset(align, in.top, parentHeight, height);
                }

                Rectangle bounds = child.getBounds();

                if (bounds.x != xOffset || bounds.y != yOffset || bounds.width != width || bounds.height != height) {
                    child.setBounds(xOffset, yOffset, width, height);
                }

//                System.out.println("\t" + child + "\n\t\t" + child.getBounds());

                if (vertical) {
                    yOffset += height;
                } else {
                    xOffset += width;
                }
            }
        }
    }

    private int limitedSize(boolean stretch, int pref, int parent) {
        if (stretch) {
            return BaseUtils.max(pref, parent);
        }

        return pref;
    }

    private int getAlignmentOffset(FlexAlignment alignment, int zeroOffset, int totalSize, int componentSize) {
        switch (alignment) {
            case START: return zeroOffset;
            case CENTER: return zeroOffset + Math.max(0, (totalSize - componentSize)/2);
            case END: return zeroOffset + Math.max(0, totalSize - componentSize);
            case STRETCH: return zeroOffset;
            default :
                throw new IllegalStateException("Wrong alignment value");
        }
    }
}
