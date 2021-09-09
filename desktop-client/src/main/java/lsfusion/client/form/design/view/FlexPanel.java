package lsfusion.client.form.design.view;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.design.view.widget.PanelWidget;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.base.view.FlexLayout;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class FlexPanel extends PanelWidget implements MouseMotionListener {

    protected static FlexPanelImpl impl = FlexPanelImpl.get();

    private final boolean vertical;

    private static Map<Widget, LayoutData> layoutDataMap = new HashMap<>();

    public static LayoutData getLayoutData(Widget widget) {
        return layoutDataMap.get(widget);
    }

    public static void setLayoutData(Widget widget, LayoutData layoutData) {
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
                checkResizeEvent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                checkResizeEvent(e);
            }
        });
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        checkResizeEvent(e);
    }

    public void checkResizeEvent(MouseEvent e) {
        checkResizeEvent(e, FlexPanel.this, true);
    }
    public void checkResizeEvent(MouseEvent e, Component cursorElement, boolean propagateToUpper) {
        ResizeHandler.checkResizeEvent(resizeHelper, cursorElement, e);

        if(propagateToUpper && !e.isConsumed()) {
            Container parent = this;
            while(true) {
                parent = parent.getParent();
                if(parent instanceof FlexPanel || parent == null)
                    break;
            }
            if (parent != null) {
                ((FlexPanel) parent).checkResizeEvent(e);
            }
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
                checkResizeEvent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                checkResizeEvent(e);
            }
        });

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        checkResizeEvent(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        checkResizeEvent(e);
    }

    public boolean isVertical() {
        return vertical;
    }

    public void add(Widget widget) {
        add(widget, FlexAlignment.START);
    }

    public void add(Widget widget, FlexAlignment alignment) {
        add(widget, getChildrenCount(), alignment);
    }

    private int getChildrenCount() {
        return getChildren().size();
    }

    public void add(Widget widget, int beforeIndex, FlexAlignment alignment) {
        add(widget, beforeIndex, alignment, 0, null); // maybe here it also makes sense to set basis to 0 as in addFill, but for now it's used mostly in vertical container for simple components
    }

    public void addFillFlex(Widget widget, Integer flexBasis) {
        addFill(widget, getChildrenCount(), flexBasis);
    }

    public void addFill(Widget widget) {
        addFill(widget, getChildrenCount());;
    }

    public void addFill(Widget widget, int beforeIndex) {
        addFill(widget, beforeIndex, null);
    }

    public void addFill(Widget widget, int beforeIndex, Integer flexBasis) {
        add(widget, beforeIndex, FlexAlignment.STRETCH, 1, flexBasis);
    }

    public void add(Widget widget, FlexAlignment alignment, double flex) {
        add(widget, alignment, flex, null);
    }

    public void add(Widget widget, FlexAlignment alignment, double flex, Integer flexBasis) {
        add(widget, getChildrenCount(), alignment, flex, flexBasis);
    }

    //main add method
    public void add(Widget widget, int beforeIndex, FlexAlignment alignment, double flex, Integer flexBasis) {
        add(widget.getComponent(), new FlexConstraints(alignment, flex), beforeIndex);

        LayoutData layoutData = impl.insertChild(widget, beforeIndex, alignment, flex, flexBasis, vertical);
        setLayoutData(widget, layoutData);
    }

    public void remove(Widget widget) {
        remove(widget.getComponent());
    }

    public static void setBaseSize(Widget widget, boolean vertical, Integer size) {
        setBaseSize(widget, vertical, size, false);
    }

    public static void setBaseSize(Widget element, boolean vertical, Integer size, boolean oppositeAndFixed) {
        if (vertical) {
            element.setComponentSize(new Dimension(element.getComponentSize().width, size != null ? size : -1)); //set minHeight
//            if (oppositeAndFixed && size != null) {
//                element.setSize(new Dimension(element.getSize().width, size)); //set height
//            }
        } else {
            element.setComponentSize(new Dimension(size != null ? size : -1, element.getComponentSize().height)); //set minWidth
//            if (oppositeAndFixed && size != null) {
//                element.setSize(new Dimension(size, element.getSize().height)); //set width
//            }
        }
    }

    private ResizeHelper resizeHelper = new ResizeHelper() {
        @Override
        public int getChildCount() {
            return getChildrenCount();
        }

        @Override
        public Component getChildElement(int index) {
            return getChildren().get(index).getComponent();
        }

        @Override
        public Widget getChildWidget(int index) {
            return getChildren().get(index);
        }

        @Override
        public void resizeChild(int index, int delta) {
            resizeWidget(index, delta);
        }
        @Override
        public boolean isChildResizable(int index) {
            if(!isWidgetResizable(index))
                return false;

            // optimization, if it is the last element, and there is a "resizable" parent, we consider this element to be not resizable (assuming that this "resizable" parent will be resized)
            if(index == getChildrenCount() - 1 && getParentSameFlexPanel(vertical) != null)
                return false;

            return true;
        }

        @Override
        public boolean isChildVisible(int index) {
            return getChildren().get(index).isVisible();
        }

        @Override
        public boolean isVertical() {
            return vertical;
        }
    };

    // the resize algorithm assumes that there should be flex column to the left, to make
    public boolean isWidgetResizable(int widgetNumber) {
        List<Widget> children = getChildren();
        for (int i = widgetNumber; i >= 0; i--) {
            Widget child = children.get(i);
            LayoutData layoutData = getLayoutData(child);
            if (layoutData != null && layoutData.baseFlex > 0 && child.isVisible())
                return true;
        }
        return false;
    }

    //ignore FilterView
    private List<Widget> getChildren() {
        return BaseUtils.immutableCast(Arrays.stream(getComponents()).filter(component -> component instanceof Widget).collect(Collectors.toList()));
    }

    @Override
    public void add(Component comp, Object constraints) {
        throw new UnsupportedOperationException("main add method should be used instead");
//        super.add(comp, constraints);
    }

    // we need to guarantee somehow that resizing this parent container will lead to the same resizing of this container
    public Pair<FlexPanel, Integer> getParentSameFlexPanel(boolean vertical) {
//        if(1==1) return null;
        Container parent = getParent();
        if(!(parent instanceof FlexPanel)) // it's some strange layouting, ignore it
            return null;

        FlexPanel flexParent = (FlexPanel) parent;
        if(vertical != flexParent.vertical) {
            LayoutData layoutData = getLayoutData(this);
            if(layoutData.alignment == FlexAlignment.STRETCH)
                return flexParent.getParentSameFlexPanel(vertical);
        } else {
            int index = flexParent.getChildren().indexOf(this);
            if(flexParent.isWidgetResizable(index))
                return new Pair<>(flexParent, index);
        }
        return null;
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

    public void resizeWidget(int widgetNumber, double delta) {
        Pair<FlexPanel, Integer> parentSameFlexPanel = getParentSameFlexPanel(vertical);

        List<Widget> children = new ArrayList<>();
        int i = 0;
        int invisibleBefore = 0;
        for(Widget child : getChildren()) {
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
        for(Widget widget : children) {
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
        for(Widget widget : children) {
            LayoutData layoutData = getLayoutData(widget);

            int realSize = impl.getSize(widget.getComponent(), vertical); // calculating size
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
            parentSameFlexPanel.first.resizeWidget(parentSameFlexPanel.second, restDelta);

        i = 0;
        for(Widget widget : children) {
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
