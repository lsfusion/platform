package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.CachableLayout;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

import static java.lang.Math.max;
import static lsfusion.interop.form.layout.CachableLayout.*;

public class SplitClientContainerView extends AbstractClientContainerView {

    private final ContainerViewPanel panel;
    private final SplitPane splitPane;
    private final JPanel hiddenHolderPanel;

    private Component leftView;
    private Component rightView;

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

        panel = new ContainerViewPanel(new BorderLayout(), false);
        panel.add(hiddenHolderPanel, BorderLayout.SOUTH);
    }

    @Override
    public void addImpl(int index, ClientComponent child, Component view) {
        if (container.children.get(0) == child) {
            setLeftComponent(child, view);
        } else if (container.children.get(1) == child) {
            setRightComponent(child, view);
        }
    }

    @Override
    public void removeImpl(int index, ClientComponent child, Component view) {
        if (container.children.get(0) == child) {
            removeLeftComponent(child, view);
        } else if (container.children.get(1) == child) {
            removeRightComponent(child, view);
        }
    }

    public void setLeftComponent(ClientComponent child, Component view) {
        leftView = view;
    }

    public void setRightComponent(ClientComponent child, Component view) {
        rightView = view;
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
            double flex1 = children.get(0).flex;
            double flex2 = children.get(1).flex;
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

    public JComponent getView() {
        return panel;
    }

    private class SplitPane extends JSplitPane {
        public SplitPane() {
            super(container.isVerticalSplit() ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT, false);

            setBorder(null);

            ((BasicSplitPaneUI) getUI()).getDivider().setBorder(BorderFactory.createEtchedBorder());
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

            if (container.isVerticalSplit()) {
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
    }
}
