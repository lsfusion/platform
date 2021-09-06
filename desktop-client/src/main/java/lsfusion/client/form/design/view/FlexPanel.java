package lsfusion.client.form.design.view;

import lsfusion.base.Pair;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.filter.user.view.FilterView;
import lsfusion.client.form.object.table.view.ToolbarView;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.base.view.FlexLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static lsfusion.client.base.SwingUtils.overrideSize;

public class FlexPanel extends JPanel implements MouseMotionListener {

    protected static FlexPanelImpl impl = FlexPanelImpl.get();

    private final boolean vertical;

    private static Map<Component, LayoutData> layoutDataMap = new HashMap<>();

    public static LayoutData getLayoutData(Component widget) {
        return layoutDataMap.get(widget);
    }

    public static void setLayoutData(Component widget, LayoutData layoutData) {
        layoutDataMap.put(widget, layoutData);
    }

    //BorderLayout
    public FlexPanel() {
        super(new BorderLayout());
        this.vertical = false;

        this.addMouseMotionListener(this);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                System.out.println("pressed");
                checkResizeEvent(FlexPanel.this, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                System.out.println("released");
                checkResizeEvent(FlexPanel.this, e);
            }
        });
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        System.out.println(e);
        checkResizeEvent(FlexPanel.this, e);
    }

    private void checkResizeEvent(FlexPanel panel, MouseEvent e) {
        ResizeHandler.checkResizeEvent(resizeHelper, FlexPanel.this, e);

        Container parent = panel.getParent();
        if(parent instanceof FlexPanel) {
            ((FlexPanel) parent).checkResizeEvent((FlexPanel) parent, e);
        }
    }

    public FlexPanel(boolean vertical) {
        this(vertical, FlexAlignment.START);
    }

    public FlexPanel(boolean vertical, FlexAlignment alignment) {
        super(null);
        setLayout(new FlexLayout(this, vertical, alignment));
        this.vertical = vertical;

        this.addMouseMotionListener(this);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                System.out.println("pressed");
                checkResizeEvent(FlexPanel.this, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                System.out.println("released");
                checkResizeEvent(FlexPanel.this, e);
            }
        });

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        checkResizeEvent(FlexPanel.this, e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        checkResizeEvent(FlexPanel.this, e);
    }

    public boolean isVertical() {
        return vertical;
    }

    public void add(JComponent widget) {
        add(widget, FlexAlignment.START);
    }

    public void add(JComponent widget, FlexAlignment alignment) {
        add(widget, getChildren().size(), alignment);
    }

    public void add(JComponent widget, int beforeIndex, FlexAlignment alignment) {
        add(widget, beforeIndex, alignment, 0, null); // maybe here it also makes sense to set basis to 0 as in addFill, but for now it's used mostly in vertical container for simple components
    }

    public void addFillFlex(JComponent widget, Integer flexBasis) {
        addFill(widget, getChildren().size(), flexBasis);
    }

    public void addFill(JComponent widget, int beforeIndex, Integer flexBasis) {
        add(widget, beforeIndex, FlexAlignment.STRETCH, 1, flexBasis);
    }

    //main add method
    public void add(JComponent widget, int beforeIndex, FlexAlignment alignment, double flex, Integer flexBasis) {
        add(widget, new FlexConstraints(alignment, flex), beforeIndex);

        LayoutData layoutData = impl.insertChild(widget, beforeIndex, alignment, flex, flexBasis, vertical);
        setLayoutData(widget, layoutData);
    }

    public static void setBaseSize(Component widget, boolean vertical, Integer size) {
        setBaseSize(widget, vertical, size, false);
    }

    public static void setBaseSize(Component element, boolean vertical, Integer size, boolean oppositeAndFixed) {

        //todo: в вебе ещё есть clearProperty, непонятно, как это реализовать
//        String propName = vertical ? (oppositeAndFixed ? "height" : "minHeight") : (oppositeAndFixed ? "width" : "minWidth");
//        if(size != null)
//            element.getStyle().setProperty(propName, size + "px");
//        else
//            element.getStyle().clearProperty(propName);

        if(size != null) {
            if (vertical) {
                if (oppositeAndFixed) {
                    element.setSize(new Dimension(element.getSize().width, size)); //set height
                } else {
                    element.setMinimumSize(new Dimension(element.getMinimumSize().width, size)); //set minHeight
                }
            } else {
                if (oppositeAndFixed) {
                    element.setSize(new Dimension(size, element.getSize().height)); //set width
                } else {
                    element.setMinimumSize(new Dimension(size, element.getMinimumSize().height)); //set minWidth
                }
            }
        }
    }

    private ResizeHelper resizeHelper = new ResizeHelper() {
        @Override
        public int getChildCount(FlexPanel panel) {
            return panel.getChildren().size();
        }

        @Override
        public Component getChildElement(FlexPanel panel, int index) {
            return panel.getChildren().get(index);
        }

        @Override
        public Component getChildWidget(FlexPanel panel, int index) {
            return panel.getChildren().get(index);
        }

        @Override
        public void resizeChild(FlexPanel panel, int index, int delta) {
            resizeWidget(panel, index, delta);
        }
        @Override
        public boolean isChildResizable(FlexPanel panel, int index) {
            if(!isWidgetResizable(panel, index))
                return false;

            // optimization, if it is the last element, and there is a "resizable" parent, we consider this element to be not resizable (assuming that this "resizable" parent will be resized)
            if(index == panel.getChildren().size() - 1 && getParentSameFlexPanel(panel, vertical) != null)
                return false;

            return true;
        }

        @Override
        public boolean isChildVisible(FlexPanel panel, int index) {
            return panel.getChildren().get(index).isVisible();
        }

        @Override
        public boolean isVertical() {
            return vertical;
        }
    };

    // the resize algorithm assumes that there should be flex column to the left, to make
    public boolean isWidgetResizable(FlexPanel panel, int widgetNumber) {
        List<Component> children = panel.getChildren();
        for (int i = widgetNumber; i >= 0; i--) {
            Component child = children.get(i);
            LayoutData layoutData = getLayoutData(child);
            if (layoutData.baseFlex > 0 && child.isVisible())
                return true;
        }
        return false;
    }

    //ignore FilterView
    private List<Component> getChildren() {
        return Arrays.stream(getComponents()).filter(component -> !(component instanceof FilterView) && !(component instanceof ToolbarView)).collect(Collectors.toList());
    }

    // we need to guarantee somehow that resizing this parent container will lead to the same resizing of this container
    public Pair<FlexPanel, Integer> getParentSameFlexPanel(FlexPanel panel, boolean vertical) {
//        if(1==1) return null;
        Container parent = getParent();
        if(!(parent instanceof FlexPanel)) // it's some strange layouting, ignore it
            return null;

        FlexPanel flexParent = (FlexPanel) parent;
        if(vertical != flexParent.vertical) {
            LayoutData layoutData = getLayoutData(this);
            if(layoutData.alignment == FlexAlignment.STRETCH)
                return flexParent.getParentSameFlexPanel(panel, vertical);
        } else {
            int index = flexParent.getChildren().indexOf(this);
            if(flexParent.isWidgetResizable(flexParent, index))
                return new Pair<>(flexParent, index);
        }
        return null;
    }

    private Dimension componentSize;

    public void setComponentSize(Dimension componentSize) {
        this.componentSize = componentSize;
    }

    @Override
    public Dimension getPreferredSize() {
        return overrideSize(super.getPreferredSize(), componentSize);
    }

    public Dimension getMaxPreferredSize() {
        return getPreferredSize();
    }

    public static final class LayoutData {
        //public Element child;
        public FlexAlignment alignment;

        // current, both are changed in resizeWidget
        public double flex;
        public Integer flexBasis; // changed on autosize and on tab change (in this case baseFlexBasis should be change to)

        public double baseFlex;
        public Integer baseFlexBasis; // if null, than we have to get it similar to fixFlexBases by setting flexes to 0

        public void setFlexBasis(Integer flexBasis) {
            this.flexBasis = flexBasis;
            baseFlexBasis = flexBasis;
        }

        public LayoutData(/*Element child, */FlexAlignment alignment, double flex, Integer flexBasis) {
            //this.child = child;
            this.alignment = alignment;
            this.flex = flex;
            this.flexBasis = flexBasis;

            this.baseFlex = flex;
            this.baseFlexBasis = flexBasis;
        }
    }

    public void resizeWidget(FlexPanel panel, int widgetNumber, double delta) {
        Pair<FlexPanel, Integer> parentSameFlexPanel = panel.getParentSameFlexPanel(panel, vertical);

        List<Component> children = new ArrayList<>();
        int i = 0;
        int invisibleBefore = 0;
        for(Component child : panel.getChildren()) {
            if (child.isVisible())
                children.add(child);
            else if(i < widgetNumber) {
                invisibleBefore++;
            }
            i++;
        }
        widgetNumber -= invisibleBefore;

        int size = children.size();
        double[] prefs = new double[size];
        double[] flexes = new double[size];

        int[] basePrefs = new int[size];
        double[] baseFlexes = new double[size];

        i = 0;
        for(Component widget : children) {
            LayoutData layoutData = getLayoutData(widget);
            if (layoutData.flexBasis == null || layoutData.baseFlexBasis == null)
                impl.setFlex(widget, 0, null, vertical);
            if (layoutData.flexBasis != null)
                prefs[i] = layoutData.flexBasis;
            flexes[i] = layoutData.flex;
            if (layoutData.baseFlexBasis != null)
                basePrefs[i] = layoutData.baseFlexBasis;
            baseFlexes[i] = layoutData.baseFlex;
            i++;
        }

        // we'll do it in different cycles to minimize the quantity of layouting
        int margins = 0;
        i = 0;
        for(Component widget : children) {
            LayoutData layoutData = getLayoutData(widget);

            int realSize = impl.getSize(widget, vertical); // calculating size
            if(layoutData.flexBasis == null || layoutData.baseFlexBasis == null) {
                if(layoutData.flexBasis == null)
                    prefs[i] = realSize;
                if(layoutData.baseFlexBasis == null)
                    basePrefs[i] = realSize;
            }
            //margins += impl.getFullSize(element, vertical) - realSize;
            i++;
        }

//        int body = ;
        // important to calculate viewWidth before setting new flexes
        int viewWidth = impl.getSize(this, vertical) - margins;
        double restDelta = SwingUtils.calculateNewFlexes(widgetNumber, delta, viewWidth, prefs, flexes, basePrefs, baseFlexes,  parentSameFlexPanel == null);

        if(parentSameFlexPanel != null && !SwingUtils.equals(restDelta, 0.0))
            parentSameFlexPanel.first.resizeWidget(panel, parentSameFlexPanel.second, restDelta);

        i = 0;
        for(Component widget : children) {
            LayoutData layoutData = getLayoutData(widget);

            Integer newPref = (int) Math.round(prefs[i]);
            // if default (base) flex basis is auto and pref is equal to base flex basis, set flex basis to null (auto)
            if(newPref.equals(basePrefs[i]) && layoutData.baseFlexBasis == null)
                newPref = null;
            layoutData.flex = flexes[i];
            layoutData.flexBasis = newPref;
            impl.setFlex(widget, layoutData, vertical);

            i++;
        }

        //onResize();
    }
}
