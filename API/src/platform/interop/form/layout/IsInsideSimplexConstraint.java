package platform.interop.form.layout;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import java.awt.*;

class IsInsideSimplexConstraint extends SingleSimplexConstraint {


    public void fillConstraint(LpSolve solver, SimplexComponentInfo comp1, SimplexComponentInfo comp2, SimplexConstraints cons1, SimplexConstraints cons2, Component comp, Container cont) throws LpSolveException {

        // левый край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp1.L, comp2.L}, LpSolve.GE, cons2.insetsInside.left + cont.getInsets().left);

        // правый край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp2.R, comp1.R}, LpSolve.GE, cons2.insetsInside.right + cont.getInsets().right);

        // верхний край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp1.T, comp2.T}, LpSolve.GE, cons2.insetsInside.top + cont.getInsets().top);

        // нижний край
        solver.addConstraintex(2, new double[] {1, -1}, new int[] {comp2.B, comp1.B}, LpSolve.GE, cons2.insetsInside.bottom + cont.getInsets().bottom);

    }

}
