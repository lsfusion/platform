package platform.client.form;

import com.sun.java.swing.plaf.windows.WindowsSplitPaneUI;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import org.apache.log4j.Logger;
import platform.base.OSUtils;
import platform.client.SwingUtils;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;
import platform.interop.form.layout.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/**
 * @author NewUser
 */
public class SimplexLayout implements LayoutManager2, ComponentListener {
    private final static Logger logger = Logger.getLogger(SimplexLayout.class);
    public boolean disableLayout = false;
    public final static int DEFAULT = 0;
    public final static int MIN = 1;
    public final static int PREFERRED = 2;

    Dimension layoutSize;
    Dimension oldDimension;
    Map<List<Component>, Map<Component, Rectangle>> cache = new HashMap<List<Component>, Map<Component, Rectangle>>();

    List<Component> allComponents = new ArrayList<Component>();
    Map<Component, ClientComponent> constraints = new HashMap<Component, ClientComponent>();

    private Container mainContainer;

    public SimplexLayout(Container imainContainer) {
        mainContainer = imainContainer;
    }

    public SimplexLayout(Container imainContainer, ClientContainer clientComponent) {
        mainContainer = imainContainer;
        constraints.put(mainContainer, clientComponent);
    }

    public void addLayoutComponent(String name, Component comp) {
        addLayoutComponent(comp);
    }

    public void addLayoutComponent(Component comp, Object constraint) {
        addLayoutComponent(comp);
        if (constraint != null) {
            ClientComponent clientComponent = (ClientComponent) constraint;
            constraints.put(comp, clientComponent);
        }
    }

    private void addLayoutComponent(Component comp) {

        if (allComponents.indexOf(comp) == -1) {

            allComponents.add(comp);
            componentsChanged = true;
            comp.addComponentListener(this);
        }
    }

    public void removeLayoutComponent(Component comp) {

        allComponents.remove(comp);
        constraints.remove(comp);

        componentsChanged = true;
        comp.removeComponentListener(this);
    }

    public void componentResized(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    // приходится вызывать invalidate, поскольку это событие обрабатывается в EventDispatchingThread и приходит позже нормального validate
    public void componentShown(ComponentEvent e) {
        if (!componentsChanged) {
            componentsChanged = true;
            mainContainer.invalidate();
        }
    }

    public void componentHidden(ComponentEvent e) {
        if (!componentsChanged) {
            componentsChanged = true;
            if (e.getComponent() != null && e.getComponent().getParent() instanceof JTabbedPane)
                // приходится вставлять такую затычку, поскольку если быстро переключаться между вкладками, то componentHidden приходит после layoutContainer и invalidate почему-то его повторно не вызывает
                layoutContainer(mainContainer);
            else
                mainContainer.invalidate();
        }
    }

    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(100, 100);
    }

    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(1, 1);
    }

    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(10000, 10000);
    }

    private boolean componentsChanged = true;
    private List<Component> components;

    boolean fillVisibleComponents() {

        // к сожалению, в JTabbedPane нету функционала, чтобы прятать определенные Tab'ы
        // поэтому приходится их просто remove'ать из JTabbedPane, что делается в методе ClientFormTabbedPane.hide
        // при этом Component который hide'ится сразу же выпадает из иерархии контейнеров и getParent возвращает null
        // по этой причине перед выполнением механизма определения компонентов и их Parent'ов, нужно использовать абсолютно свой механизм определения Parent для ClientFormTabbedPane

        Map<Component, ClientFormTabbedPane> tabs = new HashMap<Component, ClientFormTabbedPane>();
        for (Component component : allComponents) {
            if (component instanceof ClientFormTabbedPane) {
                Set<Component> children = ((ClientFormTabbedPane)component).getAddedComponents();
                for (Component child : children)
                    tabs.put(child, (ClientFormTabbedPane)component);
            }
        }

        Set<Component> visibleComponents = new LinkedHashSet<Component>();

        components = new ArrayList<Component>();
        for (Component component : allComponents) {

            boolean hasChild = hasNotAutoHideableContainerChild(component);

            boolean shouldBeVisible = hasChild;

            Container parent = tabs.containsKey(component) ? tabs.get(component) : component.getParent();

            // предполагается, что всеми потомками JTabbedPane мы управляем сами - пряча и показывая их при необходимости
            if (parent instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) parent;
                Component selectedComponent = tabbedPane.getSelectedComponent();
                if (parent instanceof ClientFormTabbedPane && hasChild) {
                    if (tabbedPane.indexOfComponent(component) == -1)
                        ((ClientFormTabbedPane) tabbedPane).show(component);
                }
                if (parent instanceof ClientFormTabbedPane && !hasChild) {
                    if (tabbedPane.indexOfComponent(component) != -1)
                        ((ClientFormTabbedPane) tabbedPane).hide(component);
                }
                if (selectedComponent != component)
                    shouldBeVisible = false;
                component.setVisible(shouldBeVisible);
            } else {
                // также мы управляем всеми контейнерами, реализующими интерфейс AutoHideableContainer
                if (component instanceof AutoHideableContainer)
                    component.setVisible(shouldBeVisible);
            }

            if (shouldBeVisible) {
                visibleComponents.add(component);
            }
        }

        // исключаем компоненты, которых нету в иерархии у предков
        // это нужно, если какой-то объект visible, а один из его предков - нет
        // такое нужно пока исключительно для JTabbedPane, когда вкладка невидима, а ее потомки - видимы 
        for (Component component : visibleComponents) {
            Component curComp = component;
            boolean visible = true;
            while (curComp != mainContainer) {
                if (curComp == null || !visibleComponents.contains(curComp)) {
                    visible = false;
                    break;
                }
                curComp = curComp.getParent();
            }
            if (visible)
                components.add(component);
        }

        // прячем divider ClientFormSplitPane'а, если хотя бы один из его компонентов не visible
        for (Component component : visibleComponents) {
            if (component instanceof ClientFormSplitPane)
                ((WindowsSplitPaneUI) ((ClientFormSplitPane) component).getUI()).getDivider().setVisible(((ClientFormSplitPane) component).areBothVisible());
        }

        componentsChanged = false;
        return components.isEmpty();
    }

    // приходится перед добавлением некоторых объектов делать setVisible всем parent контейнерам
    // иначе, если верхний контейнер не является видимым, добавляему объекту будет проставлен setVisible в false 
    public static void showHideableContainers(Component comp) {
        if (comp == null) return;
        if (comp instanceof AutoHideableContainer)
            comp.setVisible(true);
        showHideableContainers(comp.getParent());
    }

    private boolean hasNotAutoHideableContainerChild(Component component) {

        // для children'ов JTabbedPane всегда возвращаем true, поскольку setVisible для них управляется автоматически
        if (!(component instanceof AutoHideableContainer))
            return component.getParent() instanceof JTabbedPane || component.isVisible();

        // для ClientFormTabbedPane иерархия контейнеров не несет смысла, поскольку при помощи исключения из нее делается setVisible(false)
        Set<Component> children = component instanceof ClientFormTabbedPane ?
                                        ((ClientFormTabbedPane) component).getAddedComponents() :
                                        new HashSet<Component>(Arrays.asList(((Container) component).getComponents()));
        for (Component child : children)
            // Divider - один оз child'ов ClientFormSplitPane'a
            if (hasNotAutoHideableContainerChild(child) && !(child instanceof BasicSplitPaneDivider))
                return true;

        return false;
    }

    public void layoutContainer(final Container parent) {

        if (disableLayout) return;

        if (parent != mainContainer) return;

        Dimension dimension = parent.getSize();
        boolean dimensionChanged = true;
        if (oldDimension != null) {
            dimensionChanged = (Math.abs(dimension.getHeight() - oldDimension.getHeight()) +
                    Math.abs(dimension.getWidth() - oldDimension.getWidth()) > 0);
        }

        if (!dimensionChanged && !componentsChanged) return;

        if (fillVisibleComponents()) return;

        if (!dimensionChanged) {

            Map<Component, Rectangle> cachedCoords = cache.get(components);
            if (cachedCoords != null) {
                for (Component comp : components)
                    comp.setBounds(cachedCoords.get(comp));
                return;
            }
        } else {
            oldDimension = dimension;
            cache.clear();
        }

        logger.info("Begin layoutContainer " + dimension);

        layoutSize = parent.getSize();
        double[] coords = runSimplexLayout(parent, SimplexLayout.DEFAULT);

        if (coords == null) {
            Dimension minSize = calculateMinimumSize(parent);
            layoutSize = new Dimension((int) Math.max(parent.getWidth(), minSize.getWidth() * 1.5), (int) Math.max(parent.getHeight(), minSize.getHeight()));
            parent.setPreferredSize(layoutSize);
            coords = runSimplexLayout(parent, SimplexLayout.DEFAULT);
        }

        if (coords != null) {

            setComponentsBounds(coords);

            Map<Component, Rectangle> cachedCoords = new HashMap<Component, Rectangle>();
            for (Component comp : components)
                cachedCoords.put(comp, comp.getBounds());

            cache.put(components, cachedCoords);
        }

        logger.info("End layoutContainer");
    }

    public Dimension calculatePreferredSize() {

        logger.info("Begin calculatePreferredSize");

        if (fillVisibleComponents()) return new Dimension(1, 1);

        double[] coords = runSimplexLayout(null, SimplexLayout.PREFERRED);

        int maxw = 1, maxh = 1;
        if (coords != null) {
            for (Component comp : components) {
                SimplexComponentInfo info = infos.get(comp);
                maxw = Math.max(maxw, (int) coords[info.R - 1]);
                maxh = Math.max(maxh, (int) coords[info.B - 1]);
            }
        } else {
            return SwingUtils.getUsableDeviceBounds();
        }

        logger.info("End calculatePreferredSize");

        Dimension mainPrefSize = constraints.get(mainContainer).preferredSize;
        if (mainPrefSize != null) {
            maxw = Math.max(maxw, (int)mainPrefSize.getWidth());
            maxh = Math.max(maxh, (int)mainPrefSize.getHeight());
        }
        
        return new Dimension(maxw, maxh);
    }

    public Dimension calculateMinimumSize(final Container parent) {
        // if (fillVisibleComponents()) return new Dimension(1, 1);
        double[] coords = runSimplexLayout(parent, SimplexLayout.MIN);

        int maxw = 1, maxh = 1;
        if (coords != null) {
            for (Component comp : components) {
                SimplexComponentInfo info = infos.get(comp);
                maxw = Math.max(maxw, (int) coords[info.R - 1]);
                maxh = Math.max(maxh, (int) coords[info.B - 1]);
            }
        } else {
            return SwingUtils.getUsableDeviceBounds();
        }
        return new Dimension(maxw, maxh);
    }

    // передача container null обозначает, что мы ищем preferredSize
    private double[] runSimplexLayout(Container container, int type) {

        LpSolve solver = null;

        try {

            solver = LpSolve.makeLp(0, 0);

            fillComponentVariables(solver);
            fillComponentConstraints(solver, container, type);
            fillConstraints(solver);

            fillObjFunction(solver, type);

            solver.setTimeout(5);

            solver.setVerbose(LpSolve.IMPORTANT);
            solver.setOutputfile("");

            int res = solver.solve();
            if (res < 2) {
                return solver.getPtrVariables();
            } else
                return null;

        } catch (LpSolveException ex) {
//            Logger.getLogger(SimplexLayout.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // delete the problem and free memory
            if (solver != null)
                solver.deleteLp();
        }

        return null;
    }

    private Map<Component, SimplexComponentInfo> infos;
    private SimplexComponentInfo targetInfo;

    private void fillComponentVariables(LpSolve solver) throws LpSolveException {

        infos = new HashMap<Component, SimplexComponentInfo>();

        int columnNumber = 0;

        targetInfo = new SimplexComponentInfo();
        targetInfo.L = ++columnNumber;
        targetInfo.T = ++columnNumber;
        targetInfo.R = ++columnNumber;
        targetInfo.B = ++columnNumber;

        for (Component component : components) {

            SimplexComponentInfo info = new SimplexComponentInfo();
            info.L = ++columnNumber;
            info.T = ++columnNumber;
            info.R = ++columnNumber;
            info.B = ++columnNumber;
            infos.put(component, info);
        }

        for (int i = 0; i < columnNumber; i++) {
            solver.addColumn(new double[0]);
        }

    }

    private void fillComponentConstraints(LpSolve solver, Container parent, int type) throws LpSolveException {

        solver.addConstraintex(1, new double[]{1}, new int[]{targetInfo.L}, LpSolve.EQ, 0);
        solver.addConstraintex(1, new double[]{1}, new int[]{targetInfo.T}, LpSolve.EQ, 0);

        if (type == SimplexLayout.DEFAULT) { // когда parent null - ищем preferredSize
            solver.addConstraintex(1, new double[]{1}, new int[]{targetInfo.R}, LpSolve.EQ, layoutSize.getWidth());
            solver.addConstraintex(1, new double[]{1}, new int[]{targetInfo.B}, LpSolve.EQ, layoutSize.getHeight());
        } else {
            Dimension screen = SwingUtils.getUsableDeviceBounds();
            solver.addConstraintex(1, new double[]{1}, new int[]{targetInfo.R}, LpSolve.LE, screen.getWidth());
            solver.addConstraintex(1, new double[]{1}, new int[]{targetInfo.B}, LpSolve.LE, screen.getHeight());
        }

        for (Component component : components) {

            Dimension min = component.getMinimumSize();
            Dimension max = component.getMaximumSize();

            assert min.height <= max.height && min.width <= max.width;

            SimplexComponentInfo info = infos.get(component);

            //добавляем везде 1, иначе на округлении она теряется
            solver.addConstraintex(2, new double[]{1, -1}, new int[]{info.R, info.L}, LpSolve.GE, min.width);
            solver.addConstraintex(2, new double[]{1, -1}, new int[]{info.B, info.T}, LpSolve.GE, min.height);

            //приходится убирать ограничение на макс. размер, если растягивается объект, иначе ни один растягиваться не будет
            // upd : вилимо можно и не убирать
//            if (constraints.get(component).fillHorizontal == 0)
            solver.addConstraintex(2, new double[]{1, -1}, new int[]{info.R, info.L}, LpSolve.LE, max.width + 1.0);

//            if (constraints.get(component).fillVertical == 0)
            solver.addConstraintex(2, new double[]{1, -1}, new int[]{info.B, info.T}, LpSolve.LE, max.height + 1.0);
        }

    }

    private void fillConstraints(LpSolve solver) throws LpSolveException {

        for (Component comp : components)
            fillInsideConstraint(solver, comp);

        for (Component parent : components)
            fillSiblingsConstraint(solver, parent);

        fillSiblingsConstraint(solver, mainContainer);

    }

    private void fillInsideConstraint(LpSolve solver, Component comp) throws LpSolveException {

        if (components.indexOf(comp.getParent()) != -1)
            SingleSimplexConstraint.IS_INSIDE.fillConstraint(solver, infos.get(comp), infos.get(comp.getParent()), getConstraint(comp), getConstraint(comp.getParent()), comp, comp.getParent());
        else
            SingleSimplexConstraint.IS_INSIDE.fillConstraint(solver, infos.get(comp), targetInfo, getConstraint(comp), getConstraint(comp.getParent()), comp, comp.getParent());
    }

    private SimplexConstraints<ClientComponent> getConstraint(Component comp) {
        ClientComponent component = constraints.get(comp);
        return component != null
                ? component.constraints
                : SimplexConstraints.DEFAULT_CONSTRAINT;
    }

    private void fillSiblingsConstraint(LpSolve solver, Component parent) throws LpSolveException {

        // здесь будут хранится Component в том же порядке, что и в ClientContainer children
        List<Component> contComponents = new ArrayList<Component>();

        // если для этого объекта есть свой ClientContainer
        if (constraints.get(parent) instanceof ClientContainer) {

            ClientContainer parentContainer = (ClientContainer) constraints.get(parent);

            Map<ClientComponent, Component> mapComp = new HashMap<ClientComponent, Component>();

            for (Component comp : components) {
                if (comp.getParent() == parent && constraints.containsKey(comp) && parentContainer.children.contains(constraints.get(comp))) {
                    mapComp.put(constraints.get(comp), comp);
                }
            }

            for (ClientComponent comp : parentContainer.children) {
                if (mapComp.containsKey(comp))
                    contComponents.add(mapComp.get(comp));
            }
        }

        for (Component comp : components)
            if (comp.getParent() == parent && !contComponents.contains(comp)) {
                contComponents.add(comp);
            }

        int compCount = contComponents.size();
        if (compCount < 2) return;

        SimplexConstraints parentConstraints = getConstraint(parent);

        int maxVar = parentConstraints.maxVariables;

        Map<Component, SimplexSolverDirections> vars = new HashMap<Component, SimplexSolverDirections>();

        //бъем все компоненты на группы (maxVar - 1)
        //для каждой группы создаем одну переменную направлений curDir, и одна переменная направлений globalDir для всех групп
        int maxCol = (maxVar < 3) ? 1 : ((compCount - 1) / (maxVar - 1) + 1);

        SimplexSolverDirections curDir = null;
        int curCol = 0;
        int curCount = 0;
        for (Component comp : contComponents) {

            if (curCol == 0 && maxCol > 1 && compCount - curCount > 1)
                curDir = new SimplexSolverDirections(solver, parentConstraints.childConstraints.forbDir);

            vars.put(comp, curDir);

            curCount++;
            curCol++;
            if (curCol == maxCol) {
                curCol = 0;
            }
        }

        SimplexSolverDirections globalDir = new SimplexSolverDirections(solver, parentConstraints.childConstraints.forbDir);

        // замыкаем пересечения, чтобы посчитать заведомо верные условия
        Map<Component, Map<Component, DoNotIntersectSimplexConstraint>> intersects = new HashMap<Component, Map<Component, DoNotIntersectSimplexConstraint>>();
        for (Component comp1 : contComponents) {
            intersects.put(comp1, new HashMap<Component, DoNotIntersectSimplexConstraint>());
            for (Component comp2 : contComponents) {
                DoNotIntersectSimplexConstraint cons = getConstraint(comp1).intersects.get(constraints.get(comp2));
                if (comp1 != comp2 && cons != null && cons.isStraight()) {
                    intersects.get(comp1).put(comp2, cons);
                }
            }
        }

        for (Component comp3 : contComponents)
            for (Component comp1 : contComponents) {
                DoNotIntersectSimplexConstraint inter13 = intersects.get(comp1).get(comp3);
                if (comp1 != comp3 && inter13 != null)
                    for (Component comp2 : contComponents) {
                        if (comp2 != comp1 && comp2 != comp3 && inter13.equals(intersects.get(comp3).get(comp2)))
                            intersects.get(comp1).put(comp2, inter13);
                    }
            }


        for (Component comp1 : contComponents)
            for (Component comp2 : contComponents)
                if (comp1 != comp2 && !getConstraint(comp2).intersects.containsKey(constraints.get(comp1))) {

                    if (parent instanceof ClientFormSplitPane) {
                        ClientFormSplitPane split = (ClientFormSplitPane) parent;
                        DoNotIntersectSimplexConstraint constraint = split.getOrientation() == JSplitPane.HORIZONTAL_SPLIT
                                ? DoNotIntersectSimplexConstraint.TOTHE_RIGHT
                                : DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
                        Component component1 = comp1.equals(split.getLeftComponent()) ? comp1 : comp2;
                        Component component2 = comp1.equals(split.getLeftComponent()) ? comp2 : comp1;
                        constraint.fillConstraint(solver, infos.get(component1), infos.get(component2), getConstraint(component1), getConstraint(component2), null);
                    } else if (getConstraint(comp1).intersects.containsKey(constraints.get(comp2))) {
                        getConstraint(comp1).intersects.get(constraints.get(comp2)).fillConstraint(solver, infos.get(comp1), infos.get(comp2), getConstraint(comp1), getConstraint(comp2), null);
                    } else {

                        // проверка на избыточные условия пересечения
                        if (intersects.get(comp1).get(comp2) != null || intersects.get(comp2).get(comp1) != null)
                            continue;

                        int order1 = contComponents.indexOf(comp1);
                        int order2 = contComponents.indexOf(comp2);

                        if (order1 > order2)
                            continue;

                        SimplexComponentInfo info1 = infos.get(comp1); //(order1 < order2) ? infos.get(comp1) : infos.get(comp2);
                        SimplexComponentInfo info2 = infos.get(comp2); //(order1 < order2) ? infos.get(comp2) : infos.get(comp1);

                        SimplexSolverDirections dir = globalDir;
                        // для компонент из одной "группы" используем одни и те же переменные, из разных - globalDir
                        if (vars.get(comp1) == vars.get(comp2) && vars.get(comp1) != null) dir = vars.get(comp1);

                        parentConstraints.childConstraints.fillConstraint(solver, info1, info2, getConstraint(comp1), getConstraint(comp2), (maxVar == 0 ? null : dir));
                    }

                }

    }

    // На текущей момент основными целевыми функциями являются :
    // Растянуть как можно больше в высоту пропорционально fillVertical с коэффициентом 1000
    // Растянуть как можно больше в высоту пропорционально fillHorizontal с коэффициентом 1000
    // Сделать как можно ближе к Preferred размерам с коэффициентом -100 и 100
    // Сделать как можно больше те, у кого установлен fillVertical или fillHorizontal с коэффициентом 1
    // Сделать как можно выше, левее с коэффициентом 0.01

    private void fillObjFunction(LpSolve solver, int type) throws LpSolveException {

        solver.addColumn(new double[0]);

        int colmaxw = solver.getNcolumns();
        boolean fillmaxw = false;

        solver.addColumn(new double[0]);
        int colmaxh = solver.getNcolumns();
        boolean fillmaxh = false;

        List<Double> objFnc = new ArrayList<Double>();
        for (int i = 0; i < solver.getNcolumns() + 1; i++)
            objFnc.add(0.0);

        for (Component component : components) {

            Dimension max = component.getMaximumSize();
            Dimension pref = component.getPreferredSize();
            double prefCoeffWidth = 50.0 / pref.width;
            double prefCoeffHeight = 50.0 / pref.height;

            SimplexComponentInfo info = infos.get(component);

            SimplexConstraints constraint = getConstraint(component);

            // нужно проверять на максимальный размер, иначе кнопка раскрытия дерева сильно ограничит сверху colmaxw
            if (constraint.fillHorizontal > 0 && max.getWidth() >= mainContainer.getWidth() && type != SimplexLayout.PREFERRED) {
                solver.addConstraintex(3, new double[]{1, -1, -1 * constraint.fillHorizontal}, new int[]{info.R, info.L, colmaxw}, LpSolve.GE, 0);
                fillmaxw = true;
            } else {

                // Preferred size
                if (constraint.fillHorizontal >= 0) {

                    double prefWidth = (pref.width + 1.0) * (type == SimplexLayout.PREFERRED && constraint.fillHorizontal > 1E-6 ? constraint.fillVertical : 1.0);

                    solver.addColumn(new double[0]);
                    int var = solver.getNcolumns();
                    solver.addConstraintex(1, new double[]{1}, new int[]{var}, LpSolve.GE, prefWidth);
                    solver.addConstraintex(3, new double[]{1, -1, 1}, new int[]{var, info.R, info.L}, LpSolve.GE, 0);
                    objFnc.add(-100.0 - prefCoeffWidth);

                    solver.addColumn(new double[0]);
                    var = solver.getNcolumns();
                    solver.addConstraintex(1, new double[]{1}, new int[]{var}, LpSolve.LE, prefWidth);
                    solver.addConstraintex(3, new double[]{1, -1, 1}, new int[]{var, info.R, info.L}, LpSolve.LE, 0);
                    objFnc.add(100.0 + prefCoeffWidth);
                }
            }

            if (constraint.fillVertical > 0 && max.getHeight() >= mainContainer.getHeight() && type != SimplexLayout.PREFERRED) {
                solver.addConstraintex(3, new double[]{1, -1, -1 * constraint.fillVertical}, new int[]{info.B, info.T, colmaxh}, LpSolve.GE, 0);
                fillmaxh = true;
            } else {
                // Preferred size
                if (constraint.fillVertical >= 0) {
                    double prefHeight = pref.height * (type == SimplexLayout.PREFERRED && constraint.fillVertical > 1E-6 ? constraint.fillVertical : 1.0);

                    solver.addColumn(new double[0]);
                    int var = solver.getNcolumns();
                    solver.addConstraintex(1, new double[]{1}, new int[]{var}, LpSolve.GE, prefHeight);
                    solver.addConstraintex(3, new double[]{1, -1, 1}, new int[]{var, info.B, info.T}, LpSolve.GE, 0);
                    objFnc.add(-100.0 - prefCoeffHeight);

                    solver.addColumn(new double[0]);
                    var = solver.getNcolumns();
                    solver.addConstraintex(1, new double[]{1}, new int[]{var}, LpSolve.LE, prefHeight);
                    solver.addConstraintex(3, new double[]{1, -1, 1}, new int[]{var, info.B, info.T}, LpSolve.LE, 0);
                    objFnc.add(100.0 + prefCoeffHeight);
                }
            }

            // направления и расширения до максимума
            //if (type != SimplexLayout.MIN) {
            objFnc.set(info.T, (type == SimplexLayout.PREFERRED ? -1.0 : (-constraint.directions.T + ((constraint.fillVertical > 0) ? -1 : 0.0))));
            objFnc.set(info.L, (type == SimplexLayout.PREFERRED ? -1.0 : (-constraint.directions.L + ((constraint.fillHorizontal > 0) ? -1 : 0.0))));
            objFnc.set(info.B, (type == SimplexLayout.PREFERRED ? -1.0 : (constraint.directions.B + ((constraint.fillVertical > 0) ? 1 : 0.0))));
            objFnc.set(info.R, (type == SimplexLayout.PREFERRED ? -1.0 : (constraint.directions.R + ((constraint.fillHorizontal > 0) ? 1 : 0.0))));
            // }

            if (component instanceof ClientFormSplitPane && ((ClientFormSplitPane) component).areBothVisible()) {
                ClientFormSplitPane splitPane = (ClientFormSplitPane) component;

                if (splitPane.getDividerLocation() <= 0 && splitPane.dividerPosition > 0)
                    splitPane.setDividerLocation(splitPane.dividerPosition);

                if (splitPane.getDividerLocation() > 0) {
                    SimplexComponentInfo leftInfo = infos.get(splitPane.getLeftComponent());
                    SimplexComponentInfo rightInfo = infos.get(splitPane.getRightComponent());

                    double coef1;
                    solver.addColumn(new double[0]);
                    int var1 = solver.getNcolumns();
                    solver.addColumn(new double[0]);
                    int var2 = solver.getNcolumns();
                    int[] leftIndexes, rightIndexes;

                    if (splitPane.getOrientation() == JSplitPane.VERTICAL_SPLIT) {
                        coef1 = (double) splitPane.getDividerLocation() / (double) splitPane.getHeight();
                        leftIndexes = new int[]{info.B, info.T, leftInfo.B, leftInfo.T, var1};
                        rightIndexes = new int[]{info.B, info.T, rightInfo.B, rightInfo.T, var2};
                    } else {
                        coef1 = (double) splitPane.getDividerLocation() / (double) splitPane.getWidth();
                        leftIndexes = new int[]{info.R, info.L, leftInfo.R, leftInfo.L, var1};
                        rightIndexes = new int[]{info.R, info.L, rightInfo.R, rightInfo.L, var2};
                    }

                    solver.addConstraintex(1, new double[]{1}, new int[]{var1}, LpSolve.GE, 0);
                    solver.addConstraintex(5, new double[]{coef1, -coef1, -1, 1, 1}, leftIndexes, LpSolve.GE, 0);
                    objFnc.add(-10000.0);

                    solver.addConstraintex(1, new double[]{1}, new int[]{var2}, LpSolve.GE, 0);
                    double coef2 = 1 - coef1;
                    solver.addConstraintex(5, new double[]{coef2, -coef2, -1, 1, 1}, rightIndexes, LpSolve.GE, 0);
                    objFnc.add(-10000.0);
                }
            }
        }

        if (type == SimplexLayout.DEFAULT) {
            // самое важное условие - ему выдается самый большой коэффициент
            objFnc.set(colmaxw, (fillmaxw) ? 1000.0 : 0.0);
            objFnc.set(colmaxh, (fillmaxh) ? 1000.0 : 0.0);
        }
        //}

        if (type == SimplexLayout.MIN) {
            objFnc.set(targetInfo.R, -layoutSize.getHeight());
            objFnc.set(targetInfo.B, -layoutSize.getWidth());
        }

        double[] objArr = new double[objFnc.size()];
        for (int i = 0; i < objFnc.size(); i++)
            objArr[i] = objFnc.get(i);
        solver.setObjFn(objArr);
        solver.setMaxim();
    }


    private void setComponentsBounds(double[] coords) {

        Map<Component, Integer> heights = new HashMap<Component, Integer>();
        for (Component comp : components) {

            SimplexComponentInfo info = infos.get(comp);

            int LP = 0, TP = 0;
            if (components.indexOf(comp.getParent()) != -1) {
                SimplexComponentInfo infoP = infos.get(comp.getParent());
                LP = (int) coords[infoP.L - 1];
                TP = (int) coords[infoP.T - 1];
            }

            int height = (int) Math.round(coords[info.B - 1] - coords[info.T - 1]);
            int width = (int) Math.round(coords[info.R - 1] - coords[info.L - 1]);

            Integer parentHeight = null;
            int prefTabAreaHeight = 0;
            if (comp.getParent() != null) {
                parentHeight = heights.get(comp.getParent());
                if (comp.getParent() instanceof ClientFormTabbedPane) {
                    ClientFormTabbedPane parentContainer = (ClientFormTabbedPane) comp.getParent();
                    try {
                        if (parentContainer.getTabRunCount() > 1) {
                            Method getHeightMethod = BasicTabbedPaneUI.TabbedPaneLayout.class.getDeclaredMethod("preferredTabAreaHeight", int.class, int.class);
                            getHeightMethod.setAccessible(true);
                            prefTabAreaHeight = (Integer) getHeightMethod.invoke(comp.getParent().getLayout(), SwingConstants.TOP, comp.getParent().getWidth());

                            Method getTabAreaInsetsMethod = BasicTabbedPaneUI.class.getDeclaredMethod("getTabAreaInsets", int.class);
                            getTabAreaInsetsMethod.setAccessible(true);
                            Insets tabAreaInsets = (Insets) getTabAreaInsetsMethod.invoke(parentContainer.getUI(), SwingConstants.TOP);

                            prefTabAreaHeight += 2 * (tabAreaInsets.top + tabAreaInsets.bottom);

                            prefTabAreaHeight -= parentContainer.getTabInsets().height;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else if (comp.getParent() instanceof ClientFormSplitPane) {
                    ClientFormSplitPane split = (ClientFormSplitPane) comp.getParent();
                    if (split.areBothVisible()) {
                        if (split.getDividerLocation() <= 0) {
                            int newLocation = split.getOrientation() == JSplitPane.VERTICAL_SPLIT ? height : width;
                            split.setDividerLocation(newLocation);
                        }
                        if (split.getDividerLocation() > 0)
                            split.dividerPosition = split.getDividerLocation();
                    }
                }
            }
            if (parentHeight != null) {
                Insets parentInsets = IsInsideSimplexConstraint.getComponentInsets(comp.getParent());
                parentHeight -= parentInsets.top + parentInsets.bottom;
                height = Math.min(height, parentHeight - prefTabAreaHeight);
            }
            heights.put(comp, height);
            comp.setBounds((int) Math.round(coords[info.L - 1] - LP), (int) Math.round(coords[info.T - 1] - TP), width, height);
        }

    }

    public float getLayoutAlignmentX(Container target) {
        return (float) 0.0;
    }

    public float getLayoutAlignmentY(Container target) {
        return (float) 0.0;
    }

    public void invalidateLayout(Container target) {
    }

    // приходится делать не через dropLayoutCaches, поскольку механизм invalidate() считает, что отрисовка проходит всегда очень быстро
    // в итоге invalidate срабатывает даже при добавлении / удалении объектов

    public void dropCaches() {

        oldDimension = null;
        cache.clear();
    }

    public static void loadLibraries() throws IOException {
        OSUtils.loadLibrary("lpsolve55", "/platform/client/form/", SimplexLayout.class);
        OSUtils.loadLibrary("lpsolve55j", "/platform/client/form/", SimplexLayout.class);
    }
}
