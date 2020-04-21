package lsfusion.client.form.design.view;

import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.CachableLayout;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.util.Map;

import static java.lang.Math.max;
import static javax.swing.BorderFactory.createMatteBorder;
import static lsfusion.client.base.view.SwingDefaults.getComponentBorderColor;
import static lsfusion.client.base.view.SwingDefaults.splitDividerWidth;
import static lsfusion.interop.form.design.CachableLayout.*;

public class SplitClientContainerView extends AbstractClientContainerView {

    private final ContainerViewPanel panel;
    private final SplitPane splitPane;
    private final JPanel hiddenHolderPanel;

    private JComponentPanel leftView;
    private JComponentPanel rightView;

    private JPanel leftProxy;
    private JPanel rightProxy;

    private boolean wasLVisible = false;
    private boolean wasRVisible = false;

    public SplitClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);
        assert container.isSplit();

        //такой хак с размерами (+setResizeWeight()) нужен, чтобы SplitPane корректно давала первоначальные пропорции
        leftProxy = new JPanel(new BorderLayout());
        rightProxy = new JPanel(new BorderLayout());
        leftProxy.setPreferredSize(new Dimension(1, 1));
        rightProxy.setPreferredSize(new Dimension(1, 1));
        leftProxy.setMinimumSize(new Dimension(1, 1));
        rightProxy.setMinimumSize(new Dimension(1, 1));

        splitPane = new SplitPane();
        splitPane.setLeftComponent(leftProxy);
        splitPane.setRightComponent(rightProxy);

        hiddenHolderPanel = new JPanel(null);

        panel = new ContainerViewPanel();
        panel.add(hiddenHolderPanel, BorderLayout.SOUTH);
    }

    @Override
    public void addImpl(int index, ClientComponent child, JComponentPanel view) {
        assert child.getAlignment() == FlexAlignment.STRETCH && child.getFlex() > 0;// временные assert'ы чтобы проверить обратную совместимость
        if (container.children.get(0) == child) {
            setLeftComponent(child, view);
        } else if (container.children.get(1) == child) {
            setRightComponent(child, view);
        }
    }

    @Override
    public void removeImpl(int index, ClientComponent child, JComponentPanel view) {
        if (container.children.get(0) == child) {
            removeLeftComponent(child, view);
        } else if (container.children.get(1) == child) {
            removeRightComponent(child, view);
        }
    }

    public void setLeftComponent(ClientComponent child, JComponentPanel view) {
        leftView = view;
        wasLVisible = false;
        add(hiddenHolderPanel, view, -1, null, child);
    }

    public void setRightComponent(ClientComponent child, JComponentPanel view) {
        rightView = view;
        wasRVisible = false;
        add(hiddenHolderPanel, view, -1, null, child);
    }

    private void removeLeftComponent(ClientComponent child, Component view) {
        leftView = null;

        //view может быть в одной из трёх панелей
        leftProxy.remove(view);
        panel.remove(view);
        hiddenHolderPanel.remove(view);
    }

    private void removeRightComponent(ClientComponent child, Component view) {
        rightView = null;

        //view может быть в одной из трёх панелей
        rightProxy.remove(view);
        panel.remove(view);
        hiddenHolderPanel.remove(view);
    }

    @Override
    public void updateLayout() {
        boolean lVisible = leftView != null && leftView.isVisible();
        boolean rVisible = rightView != null && rightView.isVisible();

        if (lVisible && rVisible) {
            double flex1 = children.get(0).getFlex();
            double flex2 = children.get(1).getFlex();
            if (flex1 == 0 && flex2 == 0) {
                flex1 = 1;
                flex2 = 1;
            }
            splitPane.setResizeWeight(flex1 / (flex1 + flex2));

            if (!(wasLVisible && wasRVisible)) {
                leftProxy.add(leftView);
                rightProxy.add(rightView);

                panel.remove(leftView);
                panel.remove(rightView);
                panel.add(splitPane);
                panel.repaint();
            }
        } else {
            if (wasLVisible && wasRVisible) {
                panel.remove(splitPane);
            }

            if (lVisible && !(wasLVisible && !wasRVisible)) {
                panel.add(leftView);
                if (rightView != null) {
                    hiddenHolderPanel.add(rightView);
                }
            } else if (rVisible && !(wasRVisible && !wasLVisible)) {
                panel.add(rightView);
                if (leftView != null) {
                    hiddenHolderPanel.add(leftView);
                }
            }
        }

        wasLVisible = lVisible;
        wasRVisible = rVisible;
    }

    public ContainerViewPanel getPanel() {
        return panel;
    }
    
    public boolean isVertical() {
        return container.isSplitVertical();
    }

    private class SplitPane extends JSplitPane {
        public SplitPane() {
            super(isVertical() ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT, false);

            setBorder(null);

            setDividerSize(splitDividerWidth());
            updateDividerBorder();

            //удаляем default actions
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getF6(), "none");
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getF8(), "none");
        }

        @Override
        public Dimension getPreferredSize() {
            return layoutSize(super.getPreferredSize(), prefSizeGetter);
        }

        @Override
        public Dimension getMinimumSize() {
            return layoutSize(super.getPreferredSize(), minSizeGetter);
        }

        private Dimension layoutSize(Dimension result, CachableLayout.ComponentSizeGetter sizeGetter) {
            Dimension left = leftView != null ? sizeGetter.get(leftView) : new Dimension(0, 0);
            Dimension right = rightView != null ? sizeGetter.get(rightView) : new Dimension(0, 0);

            if (isVertical()) {
                result.width = limitedSum(result.width, max(left.width, right.width));
                result.height = limitedSum(result.height, left.height, right.height);
            } else {
                result.width = limitedSum(result.width, left.width, right.width);
                result.height = limitedSum(result.height, max(left.height, right.height));
            }

            return result;
        }

        @Override
        public boolean isValidateRoot() {
            //не останавливаемся в поиске validate-root, а идём дальше вверх до верхнего контейнера
            return false;
        }
        
        private void updateDividerBorder() {
            int hBorderWidth = isVertical() ? 1 : 0;
            int vBorderWidth = isVertical() ? 0 : 1;
            Border dividerBorder = createMatteBorder(hBorderWidth, vBorderWidth, hBorderWidth, vBorderWidth, getComponentBorderColor());
            ((BasicSplitPaneUI) getUI()).getDivider().setBorder(dividerBorder);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            updateDividerBorder();
        }
    }

    public Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews) {
        Dimension pref = super.getMaxPreferredSize(containerViews);

        if (isVertical()) {
            pref.height += splitDividerWidth(); // пока хардкодим ширину сплиттера (в preferred size она вообще не учитывается) 
        } else {
            pref.width += splitDividerWidth();
        }

        return pref;
    }

}
