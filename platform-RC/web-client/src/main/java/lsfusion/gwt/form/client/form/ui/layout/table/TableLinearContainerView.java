package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.*;
import lsfusion.gwt.form.client.form.ui.layout.GAbstractContainerView;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.Map;

import static com.google.gwt.user.client.ui.HasHorizontalAlignment.*;
import static com.google.gwt.user.client.ui.HasVerticalAlignment.*;

public class TableLinearContainerView extends GAbstractContainerView {
    protected final CellPanel panel;

    protected final Widget view;
    protected final FlexPanel.Justify justify;
    protected final boolean vertical;

    protected final DivWidget startFill = new DivWidget();
    protected final DivWidget endFill = new DivWidget();

    protected boolean startFillAdded = false;
    protected boolean endFillAdded = false;

    public TableLinearContainerView(GContainer container) {
        super(container);

        assert container.isLinear();

        vertical = container.isVertical();
        justify = container.getFlexJustify();

        panel = vertical ? new ResizableVerticalPanel() : new ResizableHorizontalPanel();
        panel.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        view = wrapWithTableCaption(panel);
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        if (startFillAdded) {
            index++;
        }
        ((InsertPanel)panel).insert(view, index);
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        panel.remove(view);
    }

    @Override
    public Widget getView() {
        return view;
    }

    @Override
    public void updateLayout() {
        int childCnt = childrenViews.size();
        double sumFlex = 0;
        for (int i = 0; i < childCnt; i++) {
            GComponent child = children.get(i);
            Widget childView = childrenViews.get(i);
            if (childView.isVisible()) {
                sumFlex += child.flex;
            }
        }

        if (sumFlex == 0) {
            if (!startFillAdded && justify != FlexPanel.Justify.LEADING) {
                ((InsertPanel)panel).insert(startFill, 0);
                setCellMainSize(startFill, "100%");
                startFillAdded = true;
            }
            if (!endFillAdded && justify != FlexPanel.Justify.TRAILING) {
                panel.add(endFill);
                setCellMainSize(endFill, "100%");
                endFillAdded = true;
            }
        } else {
            if (startFillAdded) {
                panel.remove(startFill);
                startFillAdded = false;
            }
            if (endFillAdded) {
                panel.remove(endFill);
                endFillAdded = false;
            }
        }

        for (int i = 0; i < childCnt; i++) {
            GComponent child = children.get(i);
            Widget childView = childrenViews.get(i);
            if (childView.isVisible()) {
                setCellPaddings(childView, child.marginTop, child.marginBottom, child.marginLeft, child.marginRight);

                if (child.flex > 0) {
                    setCellMainSize(childView, child.flex / sumFlex * 100 + "%");
                    setMainSize(childView, "100%");
                } else {
                    setCellMainSize(childView, "auto");
                    if (vertical) {
                        // по-хорошему это не нужно,
                        // но в Chrome иначе не хотят расширяться внутренние элементы этого childView (если childView - контейнер)
                        setMainSize(childView, "100%");
                    }
                }

                if (child.alignment == GFlexAlignment.STRETCH) {
                    setCellSecondarySize(childView, "100%");
                    setSecondarySize(childView, "100%");
                } else {
                    setCellSecondarySize(childView, "auto");

                    if (vertical) {
                        if (child.alignment == GFlexAlignment.TRAILING) {
                            childView.getElement().getStyle().setFloat(Style.Float.RIGHT);
                        } else if (child.alignment == GFlexAlignment.CENTER) {
                            //todo: might be done using div { display:inline-block; } + td align='..' {font-size: 0; } to remove extra margin
                            childView.getElement().getStyle().setProperty("margin", "auto");
                        } else if (child.alignment == GFlexAlignment.LEADING) {
                            childView.getElement().getStyle().setFloat(Style.Float.LEFT);
                        }
//                        panel.setCellHorizontalAlignment(childView, getCellHorzAlignment(child.alignment));
                    } else {
                        panel.setCellVerticalAlignment(childView, getCellVertAlignment(child.alignment));
                    }
                }
            } else {
                setCellMainSize(childView, "0px");
                setCellPaddings(childView, 0, 0, 0, 0);
            }
        }
    }

    private void setCellPaddings(Widget childView, int marginTop, int marginBottom, int marginLeft, int marginRight) {
        GwtClientUtils.installPaddings(childView.getElement().getParentElement(), marginTop, marginBottom, marginLeft, marginRight);
    }

    public VerticalAlignmentConstant getCellVertAlignment(GFlexAlignment alignment) {
        switch (alignment) {
            case LEADING: return ALIGN_TOP;
            case CENTER: return ALIGN_MIDDLE;
            case TRAILING: return ALIGN_BOTTOM;
        }
        return ALIGN_MIDDLE;
    }

    public HorizontalAlignmentConstant getCellHorzAlignment(GFlexAlignment alignment) {
        switch (alignment) {
            case LEADING: return ALIGN_LEFT;
            case CENTER: return ALIGN_CENTER;
            case TRAILING: return ALIGN_RIGHT;
        }
        return ALIGN_CENTER;
    }

    private void setMainSize(Widget child, String size) {
        setChildSize(vertical, child, size);
    }

    private void setSecondarySize(Widget child, String size) {
        setChildSize(!vertical, child, size);
    }

    private void setChildSize(boolean height, Widget child, String size) {
        if (height) {
            child.setHeight(size);
        } else {
            child.setWidth(size);
        }
    }

    private void setCellMainSize(Widget child, String size) {
        setCellSize(vertical, child, size);
    }

    private void setCellSecondarySize(Widget child, String size) {
        setCellSize(!vertical, child, size);
    }

    private void setCellSize(boolean height, Widget child, String size) {
        // replacement of setCellHeight(-Width). td size attributes are deprecated - '0', '*', 'auto' cause crash in IE
        Style style = child.getElement().getParentElement().getStyle();
        if (height) {
            style.setProperty("height", size);
        } else {
            style.setProperty("width", size);
        }
    }

    @Override
    public Dimension getPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        Dimension result = getChildrenStackSize(containerViews, vertical);
        int delta = 0;
        if (endFillAdded) {
            delta++;
        }
        if (startFillAdded) {
            delta++;
        }
        if (vertical) {
            result.height += delta;
        } else {
            result.width += delta;
        }
        return result;
    }
}
