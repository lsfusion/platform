package lsfusion.client.form.layout;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.CachableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public class TabbedClientContainerView extends AbstractClientContainerView {

    private final ContainerViewPanel panel;
    private final JPanel hiddenHolderPanel;
    private final TabbedPane tabbedPane;

    private ClientFormController form;

    private ClientComponent currentChild;

    private List<ClientComponent> visibleChildren = new ArrayList<ClientComponent>();

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

        panel = new ContainerViewPanel(new BorderLayout(), false);
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
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex != -1) {
                    ClientComponent selectedChild = visibleChildren.get(selectedIndex);

                    // вообще changeListener может вызваться при инициализации, но это проверка в том числе позволяет suppres'ить этот случай
                    if (selectedChild != currentChild) {
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

        tabbedPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    tabbedPane.transferFocus();
                }
            }
        });
    }

    @Override
    public void addImpl(int index, ClientComponent child, Component view) {
        hiddenHolderPanel.add(view);
    }

    @Override
    public void removeImpl(int index, ClientComponent child, Component view) {
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
                    tabbedPane.addTab(index, child.getCaption(), childView);
                }
            } else if (index != -1) {
                visibleChildren.remove(index);
                hiddenHolderPanel.add(childView);
                tabbedPane.removeTab(index);
            }
        }
    }

    public JComponent getView() {
        return panel;
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

        public void addTab(int index, String caption, Component childView) {
            // добавляем не сам компонент, а proxyPanel, чтобы TabbedPane не управляла его видимостью и не мешала логике autohide'ов
            JPanel proxyPanel = new JPanel(new BorderLayout());

            proxyPanel.add(childView);

            insertTab(caption, null, proxyPanel, null, index);
        }

        public void removeTab(int index) {
            removeTabAt(index);
        }
    }
}
