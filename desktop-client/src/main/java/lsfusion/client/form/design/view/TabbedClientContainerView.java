package lsfusion.client.form.design.view;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.form.design.Alignment;
import lsfusion.interop.form.design.CachableLayout;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;

public class TabbedClientContainerView extends AbstractClientContainerView {

    private final ClientFormController form;

    private final ContainerViewPanel panel;
    private final JPanel hiddenHolderPanel;
    private final TabbedPane tabbedPane;

    private final List<ClientComponent> visibleChildren = new ArrayList<>();

    private ClientComponent currentChild;

    public TabbedClientContainerView(ClientFormLayout formLayout, ClientContainer icontainer, ClientFormController iform) {
        super(formLayout, icontainer);
        assert container.isTabbed();

        this.form = iform;

        // чтобы логика autohide работала нормально, нужно чтобы компоненты постоянно оставались в иерархии,
        // чтобы от них нормально приходили invalidate'ы
        // поэтому при удалении таба (при этом сам компонент удаляется из tabbedPane),
        // компонент переносится в панель, которая ничего не лэйаутит и не влияет на размеры, но зато всегда присутсвует в иерархии

        tabbedPane = new TabbedPane();

        hiddenHolderPanel = new JPanel(null);

        panel = new ContainerViewPanel();
        panel.add(tabbedPane, BorderLayout.CENTER);
        panel.add(hiddenHolderPanel, BorderLayout.SOUTH);

        if (container.children.size() > 0) {
            currentChild = container.children.get(0);
        }

        initUIHandlers();
    }

    private void initUIHandlers() {
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int selectedIndex = tabbedPane.getSelectedIndex();
                        if (selectedIndex != -1) {
                            ClientComponent selectedChild = visibleChildren.get(selectedIndex);

                            // вообще changeListener может вызваться при инициализации, но это проверка в том числе позволяет suppres'ить этот случай
                            if (currentChild != selectedChild) {
                                try {
                                    currentChild = selectedChild;
                                    form.setTabVisible(container, currentChild);
                                } catch (IOException ex) {
                                    throw Throwables.propagate(ex);
                                }
                            }
                        }
                    }
                });
            }
        });

        tabbedPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyStrokes.isEnterEvent(e)) {
                    tabbedPane.transferFocus();
                }
            }
        });
    }

    @Override
    public void addImpl(int index, ClientComponent child, JComponentPanel view) {
        hiddenHolderPanel.add(view);
    }

    @Override
    public void removeImpl(int index, ClientComponent child, JComponentPanel view) {
        int visibleIndex = visibleChildren.indexOf(child);
        if (visibleIndex != -1) {
            visibleChildren.remove(visibleIndex);
            tabbedPane.removeTab(visibleIndex);
        }
    }

    @Override
    public void updateLayout() {
        int childCnt = childrenViews.size();
        for (int i = 0; i < childCnt; i++) {
            ClientComponent child = children.get(i);
            Component childView = childrenViews.get(i);

            int index = visibleChildren.indexOf(child);
            if (childView.isVisible()) {
                if (index == -1) {
                    index = BaseUtils.relativePosition(child, children, visibleChildren);
                    visibleChildren.add(index, child);
                    tabbedPane.addTab(index, child, childView);
                }
            } else if (index != -1) {
                visibleChildren.remove(index);
                hiddenHolderPanel.add(childView);
                tabbedPane.removeTab(index);
            }
        }
    }

    public JComponentPanel getView() {
        return panel;
    }

    public void activateTab(int index) {
        currentChild = container.children.get(index); // изменение сервер уже в курсе об изменениях, поэтому пометим так
        tabbedPane.activateTab(index);
    }

    public class TabbedPane extends JTabbedPane {
        //JTabbedPane.getPrefferedSize() возвращает некорректное значение,
        //приходится вот так хардкодить, чтобы его компенсировать
        private static final int hackedTabInsetsHeight = 6;

        public TabbedPane() {
            super(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        }

        @Override
        public Dimension getMinimumSize() {
            return layoutSize(CachableLayout.minSizeGetter);
        }

        @Override
        public Dimension getPreferredSize() {
            return layoutSize(CachableLayout.prefSizeGetter);
        }

        private Dimension layoutSize(CachableLayout.ComponentSizeGetter sizeGetter) {
            Dimension pref = super.getPreferredSize();

            //заново считаем максимальный размер и вычитаем его, т. к. размеры таб-панели зависят от LAF
            int maxWidth = 0;
            int maxHeight = 0;
            for (int i = 0; i < getTabCount(); i++) {
                Component child = getComponentAt(i);
                if (child != null) {
                    Dimension size = sizeGetter.get(child);
                    if (size != null) {
                        maxWidth = max(maxWidth, size.width);
                        maxHeight = max(maxHeight, size.height);
                    }
                }
            }
            pref.width -= maxWidth;
            pref.height -= maxHeight;

            Component selected = getSelectedComponent();
            if (selected != null) {
                Dimension d = sizeGetter.get(selected);
                pref.width += d.width;
                pref.height += d.height;
            }

            pref.height += hackedTabInsetsHeight;

            return pref;
        }

        public void addTab(int index, ClientComponent child, Component childView) {
            // добавляем не сам компонент, а proxyPanel, чтобы TabbedPane не управляла его видимостью и не мешала логике autohide'ов
            JComponentPanel proxyPanel = new JComponentPanel(true, Alignment.START) {
                @Override
                public void validate() {
                    super.validate();
                    updatePageSizes(this);
                }
            };
            
            proxyPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

            proxyPanel.add(childView, new FlexConstraints(child.getAlignment(), child.getFlex()));

            insertTab(child.getCaption(), null, proxyPanel, null, index);
        }

        private void activateTab(int index) {
            setSelectedIndex(index);
        }

        private void updatePageSizes(Container container) {
            if (container.isVisible()) {
                int childCnt = container.getComponentCount();
                for (int i = 0; i < childCnt; ++i) {
                    Component child = container.getComponent(i);
                    if (child instanceof GridTable) {
                        ((GridTable) child).updatePageSizeIfNeeded(false);
                    } else if (child instanceof Container) {
                        updatePageSizes((Container) child);
                    }
                }
            }
        }

        public void removeTab(int index) {
            removeTabAt(index);
        }
    }

    public boolean isTabVisible(ClientComponent tab) {
        return visibleChildren.contains(tab);
    }

    @Override
    public Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews) {
        int selected = tabbedPane.getSelectedIndex();
        if (selected != -1) {
            Dimension dimensions = getChildMaxPreferredSize(containerViews, selected);
            dimensions.height += 32 + 5; //little extra for borders, etc., пока захардкодим 32 в высоту tab header'а
            return dimensions;
        }
        return new Dimension(0, 0);
    }
}
