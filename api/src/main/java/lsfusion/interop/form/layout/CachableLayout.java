package lsfusion.interop.form.layout;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class CachableLayout<C> implements LayoutManager2, Serializable {

    public static final ComponentSizeGetter minSizeGetter = new ComponentSizeGetter() {
        @Override
        public Dimension get(Component child) { return child.getMinimumSize(); }
    };

    public static final ComponentSizeGetter prefSizeGetter = new ComponentSizeGetter() {
        @Override
        public Dimension get(Component child) { return child.getPreferredSize(); }
    };

    protected final Container target;
    protected final boolean hasConstraints;
    protected final Map<Component, C> constraintsMap;

    protected Dimension minSize;
    protected Dimension prefSize;

    protected CachableLayout(Container target) {
        this(target, true);
    }

    public CachableLayout(Container target, boolean hasConstraints) {
        this.target = target;
        this.hasConstraints = hasConstraints;
        this.constraintsMap = hasConstraints ? new HashMap<Component, C>() : null;
    }

    protected void setConstraints(Component child, C constraints) {
        setConstraintsInternal(child, cloneConstraints(constraints));
    }

    protected void setConstraintsInternal(Component comp, C constraints) {
        constraintsMap.put(comp, constraints);
    }

    protected C lookupConstraints(Component child) {
        C constraints = constraintsMap.get(child);
        if (constraints == null) {
            setConstraintsInternal(child, constraints = getDefaultContraints());
        }
        return constraints;
    }

    protected void removeConstraints(Component child) {
        constraintsMap.remove(child);
    }

    @Override
    public void addLayoutComponent(String name, Component child) {
        if (hasConstraints) {
            throw new IllegalStateException("CachableLayout doesn't use string constraints");
        }
        invalidateLayout(target);
    }

    public void addLayoutComponent(Component child, Object constraints) {
        if (hasConstraints) {
            setConstraints(child, (C) constraints);
        }
        invalidateLayout(target);
    }

    @Override
    public void removeLayoutComponent(Component child) {
        if (hasConstraints) {
            removeConstraints(child);
        }
        invalidateLayout(target);
    }

    protected C getDefaultContraints() {
        return null;
    }

    protected C cloneConstraints(C original) {
        return null;
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
        checkParent(parent);
        if (minSize != null) {
            return minSize;
        }

        return minSize = layoutSizeWithInsets(parent, minSizeGetter);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        checkParent(parent);
        if (prefSize != null) {
            return prefSize;
        }

        return prefSize = layoutSizeWithInsets(parent, prefSizeGetter);
    }

    private Dimension layoutSizeWithInsets(Container parent, ComponentSizeGetter sizeGetter) {
        return addInsets(parent, layoutSize(parent, sizeGetter));
    }

    public static Dimension addInsets(Container container, Dimension d) {
        Insets insets = container.getInsets();
        d.width = limitedSum(d.width, insets.left, insets.right);
        d.height = limitedSum(d.height, insets.top, insets.bottom);
        return d;
    }

    protected abstract Dimension layoutSize(Container parent, ComponentSizeGetter sizeGetter);

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

    @Override
    public void invalidateLayout(Container parent) {
        checkParent(parent);
        minSize = null;
        prefSize = null;
    }

    public interface ComponentSizeGetter {
        Dimension get(Component child);
    }
}
