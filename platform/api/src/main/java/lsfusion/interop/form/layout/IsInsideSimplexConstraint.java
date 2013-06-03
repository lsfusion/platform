package lsfusion.interop.form.layout;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class IsInsideSimplexConstraint extends SingleSimplexConstraint {


    public void fillConstraint(LpSolve solver, SimplexComponentInfo comp1, SimplexComponentInfo comp2, SimplexConstraints cons1, SimplexConstraints cons2, Component comp, Container cont) throws LpSolveException {

        Insets contInsets = getContainerInsets(cont, cons2);

        // левый край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp1.L, comp2.L}, LpSolve.GE, contInsets.left);

        // правый край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp2.R, comp1.R}, LpSolve.GE, contInsets.right);

        // верхний край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp1.T, comp2.T}, LpSolve.GE, contInsets.top);

        // нижний край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp2.B, comp1.B}, LpSolve.GE, contInsets.bottom);
    }

    public static Insets getContainerInsets(Container cont, SimplexConstraints constraints) {
        Insets contInsets = cont.getInsets();

        // приходится делать именно таким образом, поскольку перегрузить getInsets нельзя - он используется LayoutManager'ом у JTabbedPane
        // будем считать, что такие JTabbedPane'ы должны имплементить InsetTabbedPane и возвращать insets'ы, которые они используют
        if (cont instanceof InsetTabbedPane) {
            InsetTabbedPane insetPane = (InsetTabbedPane) cont;
            contInsets.right += insetPane.getTabInsets().width;
            contInsets.bottom += insetPane.getTabInsets().height;
        }

        if (cont instanceof JComponent) {
            JComponent jcomp = (JComponent) cont;
            if (jcomp.getBorder() instanceof TitledBorder) {
                contInsets.top -= 6;
                contInsets.bottom -= 2;
                contInsets.left -= 2;
                contInsets.right -= 2;
            }
        }

        contInsets.top += constraints.insetsInside.top;
        contInsets.bottom += constraints.insetsInside.bottom;
        contInsets.left += constraints.insetsInside.left;
        contInsets.right += constraints.insetsInside.right;

        return contInsets;
    }
}
