/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import lpsolve.*;


/**
 *
 * @author NewUser
 */
public class SimplexLayout implements LayoutManager2 {

    public boolean disableLayout = false;

    public boolean hasChanged = true;
    Dimension oldDimension;
    Map<List<Component>,Map<Component, Rectangle>> cache = new HashMap();

    List<Component> components = new ArrayList();
    Map<Component,SimplexConstraints> constraints = new HashMap();
    
    private Container mainContainer;
    
    public SimplexLayout(Container imainContainer) {
        mainContainer = imainContainer;
        constraints.put(mainContainer, SimplexConstraints.DEFAULT_CONSTRAINT);
    }
    
    public SimplexLayout(Container imainContainer, SimplexConstraints c) {
        mainContainer = imainContainer;
        constraints.put(mainContainer, c);
    }
    
    public void addLayoutComponent(String name, Component comp) {
        if (components.indexOf(comp) == -1) components.add(comp);
        constraints.put(comp, SimplexConstraints.DEFAULT_CONSTRAINT);
        hasChanged = true;
    }

    public void addLayoutComponent(Component comp, Object constr) {
        if (components.indexOf(comp) == -1) components.add(comp);
        if (constr != null)
            constraints.put(comp, (SimplexConstraints)constr);
        else
            constraints.put(comp, SimplexConstraints.DEFAULT_CONSTRAINT);
        System.out.println("addLayoutComp");
        hasChanged = true;
    }
    
    public void removeLayoutComponent(Component comp) {
        components.remove(comp);
        constraints.remove(comp);
        System.out.println("removeLayoutComp");
        hasChanged = true;
    }

    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(500,500);
    }

    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(1,1);
    }

    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(10000,10000);
    }
    

    public void layoutContainer(final Container parent) {

        if (disableLayout) return;

        long stl = System.currentTimeMillis();

        if (parent != mainContainer) return;
        if (components.isEmpty()) return;

        if (parent.getSize().equals(oldDimension)) {

            if (!hasChanged) return;
            Map<Component,Rectangle> cachedCoords = cache.get(components);
            if (cachedCoords != null) {
                for (Component comp : components)
                    comp.setBounds(cachedCoords.get(comp));
                return;
            }
        } else {
            oldDimension = parent.getSize();
            cache.clear();
        }

        
        LpSolve solver = null;

        try {

            solver = LpSolve.makeLp(0, 0);

            fillComponentVariables(solver, parent);
            fillComponentConstraints(solver, parent);
            fillConstraints(solver);

            fillObjFunction(solver);

//            solver.setTimeout(1);
            // solve the problem
            long st = System.currentTimeMillis();

            solver.setVerbose(LpSolve.IMPORTANT);
            solver.setOutputfile("");

            int res = solver.solve();
            if (res < 2) {

                setComponentsBounds(solver.getPtrVariables());

                Map<Component,Rectangle> cachedCoords = new HashMap();
                for (Component comp : components)
                    cachedCoords.put(comp, comp.getBounds());

                cache.put(components, cachedCoords);
            }

        } catch (LpSolveException ex) {
            Logger.getLogger(SimplexLayout.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // delete the problem and free memory
            if (solver != null)
                solver.deleteLp();
        }

        hasChanged = false;
        
        System.out.println("Layout complete : " + (System.currentTimeMillis()-stl));
    }

    private Map<Component,SimplexComponentInfo> infos;
    private SimplexComponentInfo targetInfo;
    
    private void fillComponentVariables(LpSolve solver, Container parent) throws LpSolveException {

        infos = new HashMap();
        
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

    private void fillComponentConstraints(LpSolve solver, Container parent) throws LpSolveException {
        
        solver.addConstraintex(1, new double[] {1}, new int[] {targetInfo.L}, LpSolve.EQ, 0);
        solver.addConstraintex(1, new double[] {1}, new int[] {targetInfo.T}, LpSolve.EQ, 0);

        solver.addConstraintex(1, new double[] {1}, new int[] {targetInfo.R}, LpSolve.EQ, parent.getWidth());
        solver.addConstraintex(1, new double[] {1}, new int[] {targetInfo.B}, LpSolve.EQ, parent.getHeight());

        for (Component component : components) {
            
            Dimension min = component.getMinimumSize();
            Dimension max = component.getMaximumSize();
            
            SimplexComponentInfo info = infos.get(component);
                    
            solver.addConstraintex(2, new double[] {1, -1}, new int[] {info.R, info.L}, LpSolve.GE, min.width);
            solver.addConstraintex(2, new double[] {1, -1}, new int[] {info.B, info.T}, LpSolve.GE, min.height);

            //приходится убирать ограничение на макс. размер, если растягивается объект, иначе ни один растягиваться не будет
            if (constraints.get(component).fillHorizontal == 0)
                solver.addConstraintex(2, new double[] {1, -1}, new int[] {info.R, info.L}, LpSolve.LE, max.width);
            if (constraints.get(component).fillVertical == 0)
                solver.addConstraintex(2, new double[] {1, -1}, new int[] {info.B, info.T}, LpSolve.LE, max.height);
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
            SingleSimplexConstraint.IS_INSIDE.fillConstraint(solver, infos.get(comp), infos.get(comp.getParent()), constraints.get(comp), constraints.get(comp.getParent()));
        else
            SingleSimplexConstraint.IS_INSIDE.fillConstraint(solver, infos.get(comp), targetInfo, constraints.get(comp), constraints.get(targetInfo));
    }

    private void fillSiblingsConstraint(LpSolve solver, Component parent) throws LpSolveException {

        SimplexConstraints parentConstraints = constraints.get(parent);
        
        int maxVar = parentConstraints.maxVariables;
        
        int compCount = 0;
        //упорядочиваем компоненты по их order'у
        TreeMap<Integer, ArrayList<Component>> comporder = new TreeMap();
        for (Component comp : components) 
            if (comp.getParent() == parent) {
                Integer order = constraints.get(comp).order;
                ArrayList<Component> alc = comporder.get(order);
                if (alc == null) alc = new ArrayList();
                alc.add(comp);
                comporder.put(order, alc);
                compCount++;
            }
        
        if (compCount < 2) return;

        List<Component> order = new ArrayList();
        
        Map<Component, SimplexSolverDirections> vars = new HashMap();
        
        //бъем все компоненты на группы
        int maxCol = (maxVar < 3) ? 1 : ((compCount - 1) / (maxVar - 1) + 1);

        SimplexSolverDirections curDir = null;
        int curRow = 0;
        int curCol = 0;
        
        Iterator<Integer> it = comporder.navigableKeySet().iterator();
        int curCount = 0;
        while (it.hasNext()) {
            
            ArrayList<Component> complist = comporder.get(it.next());
            
            for (Component comp : complist) {
                
                if (curCol == 0 && maxCol > 1 && compCount - curCount > 1) 
                    curDir = new SimplexSolverDirections(solver, parentConstraints.childConstraints.forbDir);
                
                order.add(comp);
                vars.put(comp, curDir);
                
                curCount++;
                curCol++;
                if (curCol == maxCol) {
                    curCol = 0;
                    curRow++;
                }
            }
        }
        
        SimplexSolverDirections globalDir = new SimplexSolverDirections(solver, parentConstraints.childConstraints.forbDir);
        
        for (Component comp1 : components) 
            if (comp1.getParent() == parent)
                for (Component comp2 : components)
                    if (comp2.getParent() == parent && comp1 != comp2 && !constraints.get(comp2).containsKey(comp1)) {
                        
                        if (constraints.get(comp1).containsKey(comp2)) {
                            constraints.get(comp1).get(comp2).fillConstraint(solver, infos.get(comp1), infos.get(comp2), constraints.get(comp1), constraints.get(comp2), null);
                        } else {
                            
                            int order1 = order.indexOf(comp1);
                            int order2 = order.indexOf(comp2);
                            
                            if (order1 > order2)
                                continue;

                            SimplexComponentInfo info1 = infos.get(comp1); //(order1 < order2) ? infos.get(comp1) : infos.get(comp2);
                            SimplexComponentInfo info2 = infos.get(comp2); //(order1 < order2) ? infos.get(comp2) : infos.get(comp1);

                            SimplexSolverDirections dir = globalDir;
                            if (vars.get(comp1) == vars.get(comp2) && vars.get(comp1) != null) dir = vars.get(comp1);
                            
                            parentConstraints.childConstraints.fillConstraint(solver, info1, info2, constraints.get(comp1), constraints.get(comp2), dir);
                        }

                    }
        
    }
    
    private void fillObjFunction(LpSolve solver) throws LpSolveException {

        solver.addColumn(new double[0]);
        int colmaxw = solver.getNcolumns();

        solver.addColumn(new double[0]);
        int colmaxh = solver.getNcolumns();
        
        List<Double> objFnc = new ArrayList();
        for (int i = 0; i < solver.getNcolumns()+1; i++)
            objFnc.add(0.0);
            
//        double[] objFnc = new double[solver.getNcolumns()+1];
        
//        List<Integer> extraObjFnc;
        
        for (Component component : components) {
            
            Dimension pref = component.getPreferredSize();
            
            SimplexComponentInfo info = infos.get(component);
            
            SimplexConstraints constraint = constraints.get(component);
            
            if (constraint.fillHorizontal > 0) {
                solver.addConstraintex(3, new double[] {1, -1, -1 * constraint.fillHorizontal}, new int[] {info.R, info.L, colmaxw}, LpSolve.GE, 0);
            } else {
                
                solver.addColumn(new double[0]);
                int var = solver.getNcolumns();
                solver.addConstraintex(1, new double[] {1}, new int[] {var}, LpSolve.GE, pref.width);
                solver.addConstraintex(3, new double[] {1, -1, 1}, new int[] {var, info.R, info.L}, LpSolve.GE, 0);
                objFnc.add(-1.0);

                solver.addColumn(new double[0]);
                var = solver.getNcolumns();
                solver.addConstraintex(1, new double[] {1}, new int[] {var}, LpSolve.LE, pref.width);
                solver.addConstraintex(3, new double[] {1, -1, 1}, new int[] {var, info.R, info.L}, LpSolve.LE, 0);
                objFnc.add(1.0);
            }
                
            if (constraint.fillVertical > 0) {
                solver.addConstraintex(3, new double[] {1, -1, -1 * constraint.fillVertical}, new int[] {info.B, info.T, colmaxh}, LpSolve.GE, 0);
            } else {
                
                solver.addColumn(new double[0]);
                int var = solver.getNcolumns();
                solver.addConstraintex(1, new double[] {1}, new int[] {var}, LpSolve.GE, pref.height);
                solver.addConstraintex(3, new double[] {1, -1, 1}, new int[] {var, info.B, info.T}, LpSolve.GE, 0);
                objFnc.add(-1.0);

                solver.addColumn(new double[0]);
                var = solver.getNcolumns();
                solver.addConstraintex(1, new double[] {1}, new int[] {var}, LpSolve.LE, pref.height);
                solver.addConstraintex(3, new double[] {1, -1, 1}, new int[] {var, info.B, info.T}, LpSolve.LE, 0);
                objFnc.add(1.0);
            }
            
            objFnc.set(info.T, -constraint.directions.T);
            objFnc.set(info.L, -constraint.directions.L);
            objFnc.set(info.B, constraint.directions.B);
            objFnc.set(info.R, constraint.directions.R);
                    
        }
        
        objFnc.set(colmaxw, 5.0);
        objFnc.set(colmaxh, 5.0);
        
        double[] objArr = new double[objFnc.size()];
        for (int i = 0; i < objFnc.size(); i++)
            objArr[i] = objFnc.get(i);
        solver.setObjFn(objArr);
//        solver.setObjFnex(2, new double[] {1, 1}, new int[] {maxcolw, maxcolh});
        solver.setMaxim();
    }

   
    private void setComponentsBounds(double[] coords) {
        
        for (Component comp : components) {
            
            SimplexComponentInfo info = infos.get(comp);
            
            int LP = 0, TP = 0;
            if (components.indexOf(comp.getParent()) != -1) {
                SimplexComponentInfo infoP = infos.get(comp.getParent());
                LP = (int) coords[infoP.L-1];
                TP = (int) coords[infoP.T-1];
            }
            
            comp.setBounds((int)coords[info.L-1]-LP, (int)coords[info.T-1]-TP, 
                           (int)(coords[info.R-1]-coords[info.L-1]), (int)(coords[info.B-1]-coords[info.T-1]));
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

    
}
class SimplexConstraints extends HashMap<Component, DoNotIntersectSimplexConstraint> {

    public int order = 0;
    
    public static final SimplexConstraints DEFAULT_CONSTRAINT = new SimplexConstraints();
    
    public DoNotIntersectSimplexConstraint childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
    public int maxVariables = 3;

//    public static int MAXIMUM = 1;
//    public static int PREFERRED = 0;
    
    public double fillVertical = 0; //PREFERRED;
    public double fillHorizontal = 0; //PREFERRED;

    public Insets insetsSibling = new Insets(4,4,4,4);
    public Insets insetsInside = new Insets(2,2,2,2);
    
    public SimplexComponentDirections directions = new SimplexComponentDirections(0.01,0.01,0,0);
    
    public SimplexConstraints() {
    }
    
}

abstract class SingleSimplexConstraint {
    
    public static final DoNotIntersectSimplexConstraint DO_NOT_INTERSECT = new DoNotIntersectSimplexConstraint();
    public static final IsInsideSimplexConstraint IS_INSIDE = new IsInsideSimplexConstraint();
    
    public static final DoNotIntersectSimplexConstraint TOTHE_RIGHTBOTTOM = new DoNotIntersectSimplexConstraint(
                                                                   DoNotIntersectSimplexConstraint.LEFT |
                                                                   DoNotIntersectSimplexConstraint.TOP);

    public static final DoNotIntersectSimplexConstraint TOTHE_RIGHT = new DoNotIntersectSimplexConstraint(
                                                               DoNotIntersectSimplexConstraint.LEFT |
                                                               DoNotIntersectSimplexConstraint.TOP |
                                                               DoNotIntersectSimplexConstraint.BOTTOM);

    public static final DoNotIntersectSimplexConstraint TOTHE_BOTTOM = new DoNotIntersectSimplexConstraint(
                                                                   DoNotIntersectSimplexConstraint.LEFT |
                                                                   DoNotIntersectSimplexConstraint.RIGHT |
                                                                   DoNotIntersectSimplexConstraint.TOP);
    
    public static int MAXVALUE = 1000000;
    
//    public abstract void fillConstraint(LpSolve solver, SimplexComponentInfo comp1, SimplexComponentInfo comp2) throws LpSolveException;
}

class DoNotIntersectSimplexConstraint extends SingleSimplexConstraint {
    
    public static int LEFT = 1;
    public static int RIGHT = 2;
    public static int TOP = 4;
    public static int BOTTOM = 8;
    
    int forbDir;
    
    public DoNotIntersectSimplexConstraint () {
        forbDir = 0;
    }
    
    public DoNotIntersectSimplexConstraint (int iforbDir) {
        forbDir = iforbDir;
    }
    
    public void fillConstraint(LpSolve solver, SimplexComponentInfo comp1, SimplexComponentInfo comp2, SimplexConstraints cons1, SimplexConstraints cons2, SimplexSolverDirections vars) throws LpSolveException {
  
        if (vars == null)
            vars = new SimplexSolverDirections(solver, forbDir);
        
        //или находится левее
        if (vars.L != null) {
            
            solver.addConstraintex(3, new double[] {MAXVALUE, -1, 1},
                                    new int[] {vars.L, comp1.L, comp2.R},
                                    LpSolve.LE, MAXVALUE - cons1.insetsSibling.left);
        }
        
        //или находится правее
        if (vars.R != null) {
            
            solver.addConstraintex(3, new double[] {MAXVALUE, -1, 1},
                                    new int[] {vars.R, comp2.L, comp1.R},
                                    LpSolve.LE, MAXVALUE - cons1.insetsSibling.right);
        }
        
        //или находится выше
        if (vars.T != null) {
            
            solver.addConstraintex(3, new double[] {MAXVALUE, -1, 1},
                                    new int[] {vars.T, comp1.T, comp2.B},
                                    LpSolve.LE, MAXVALUE  - cons1.insetsSibling.top);
        }
        
        //или находится ниже
        if (vars.B != null) {
            
            solver.addConstraintex(3, new double[] {MAXVALUE, -1, 1},
                                    new int[] {vars.B, comp2.T, comp1.B},
                                    LpSolve.LE, MAXVALUE - cons1.insetsSibling.bottom);
        }

    }
    
}

class SimplexSolverDirections {
    
    Integer L;
    Integer R;
    Integer T;
    Integer B;
    
    public SimplexSolverDirections(LpSolve solver, int forbDir) throws LpSolveException {

        int startColumn = solver.getNcolumns();

        int varCount = 0;
        List<Integer> varList = new ArrayList();
        
        //или находится левее
        if ((forbDir & DoNotIntersectSimplexConstraint.LEFT) == 0) {
            
            varCount++;
            varList.add(startColumn + varCount);
            solver.addColumn(new double[0]);
            solver.setBinary(startColumn + varCount, true);
            L = startColumn + varCount;
        }
        
        //или находится правее
        if ((forbDir & DoNotIntersectSimplexConstraint.RIGHT) == 0) {
            
            varCount++;
            varList.add(startColumn + varCount);
            solver.addColumn(new double[0]);
            solver.setBinary(startColumn + varCount, true);
            R = startColumn + varCount;
        }
        
        //или находится выше
        if ((forbDir & DoNotIntersectSimplexConstraint.TOP) == 0) {
            
            varCount++;
            varList.add(startColumn + varCount);
            solver.addColumn(new double[0]);
            solver.setBinary(startColumn + varCount, true);
            T = startColumn + varCount;
        }
        
        //или находится ниже
        if ((forbDir & DoNotIntersectSimplexConstraint.BOTTOM) == 0) {
            
            varCount++;
            varList.add(startColumn + varCount);
            solver.addColumn(new double[0]);
            solver.setBinary(startColumn + varCount, true);
            B = startColumn + varCount;
        }

        if (varCount > 1)
            System.out.println("LPSolve : addVariables - " + varCount);
        
        //задаем базовое ограничение
        double[] coeffs = new double[varCount];
        int[] colns = new int[varCount];
        for (int i = 0; i < varCount; i++) {
            
            coeffs[i] = 1;
            colns[i] = varList.get(i);
        }
        solver.addConstraintex(varCount, coeffs, colns, LpSolve.EQ, 1);
        
    }
}

class IsInsideSimplexConstraint extends SingleSimplexConstraint {
    
    
    public void fillConstraint(LpSolve solver, SimplexComponentInfo comp1, SimplexComponentInfo comp2, SimplexConstraints cons1, SimplexConstraints cons2) throws LpSolveException {
        
        // левый край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp1.L, comp2.L}, LpSolve.GE, cons1.insetsInside.left);

        // правый край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp2.R, comp1.R}, LpSolve.GE, cons1.insetsInside.right);

        // верхний край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp1.T, comp2.T}, LpSolve.GE, cons1.insetsInside.top);

        // нижний край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp2.B, comp1.B}, LpSolve.GE, cons1.insetsInside.bottom);
        
    }
    
}



class SimplexComponentInfo {

    int T;
    int L;
    int B;
    int R;

}

class SimplexComponentDirections {
    
    double T;
    double L;
    double B;
    double R;
    
    public SimplexComponentDirections(double iT, double iL, double iB, double iR) {
        
        T = iT;
        L = iL;
        B = iB;
        R = iR;
        
    }
    
}