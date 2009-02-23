package platform.client.form.layout;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

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
                                    LpSolve.LE, MAXVALUE - cons1.insetsSibling.left - cons2.insetsSibling.right);
        }

        //или находится правее
        if (vars.R != null) {

            solver.addConstraintex(3, new double[] {MAXVALUE, -1, 1},
                                    new int[] {vars.R, comp2.L, comp1.R},
                                    LpSolve.LE, MAXVALUE - cons1.insetsSibling.right - cons2.insetsSibling.left);
        }

        //или находится выше
        if (vars.T != null) {

            solver.addConstraintex(3, new double[] {MAXVALUE, -1, 1},
                                    new int[] {vars.T, comp1.T, comp2.B},
                                    LpSolve.LE, MAXVALUE  - cons1.insetsSibling.top - cons2.insetsSibling.bottom);
        }

        //или находится ниже
        if (vars.B != null) {

            solver.addConstraintex(3, new double[] {MAXVALUE, -1, 1},
                                    new int[] {vars.B, comp2.T, comp1.B},
                                    LpSolve.LE, MAXVALUE - cons1.insetsSibling.bottom - cons2.insetsSibling.top);
        }

    }

}
