package lsfusion.interop.form.layout;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

public class DoNotIntersectSimplexConstraint extends SingleSimplexConstraint {

    public final static int LEFT = 1;
    public final static int RIGHT = 2;
    public final static int TOP = 4;
    public final static int BOTTOM = 8;

    public int forbDir;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DoNotIntersectSimplexConstraint that = (DoNotIntersectSimplexConstraint) o;

        if (forbDir != that.forbDir) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return forbDir;
    }

    public DoNotIntersectSimplexConstraint () {
        forbDir = 0;
    }

    public DoNotIntersectSimplexConstraint (int iforbDir) {
        forbDir = iforbDir;
    }

    public void fillSplitConstraint(boolean isVertical, int dividerSize, LpSolve solver, SimplexComponentInfo comp1, SimplexComponentInfo comp2, SimplexConstraints cons1, SimplexConstraints cons2) throws LpSolveException {
        SimplexSolverDirections vars = new SimplexSolverDirections(solver, forbDir);

        if (isVertical) {
            assert vars.B != null;
            solver.addConstraintex(3, new double[] {MAXVALUE, -1, 1},
                                   new int[] {vars.B, comp2.T, comp1.B},
                                   LpSolve.LE, MAXVALUE - dividerSize);
        } else {
            assert vars.R != null;
            solver.addConstraintex(3, new double[] {MAXVALUE, -1, 1},
                                   new int[] {vars.R, comp2.L, comp1.R},
                                   LpSolve.LE, MAXVALUE - dividerSize);
        }
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

    public boolean isStraight() {
        return Integer.bitCount(forbDir) >= 3;
    }

    public String getConstraintCode() {
        if(this.equals(DO_NOT_INTERSECT)) {
            return "DoNotIntersectSimplexConstraint.DO_NOT_INTERSECT";
        } else if (this.equals(TOTHE_BOTTOM)) {
            return "DoNotIntersectSimplexConstraint.TOTHE_BOTTOM";
        } else if (this.equals(TOTHE_RIGHT)) {
            return "DoNotIntersectSimplexConstraint.TOTHE_RIGHT";
        } else if (this.equals(TOTHE_RIGHTBOTTOM)) {
            return "DoNotIntersectSimplexConstraint.TOTHE_RIGHTBOTTOM";
        } else {
            String result = "new DoNotIntersectSimplexConstraint(";
            result += (forbDir & TOP) != 0 ? "DoNotIntersectSimplexConstraint.TOP | " : "";
            result += (forbDir & LEFT) != 0 ? "DoNotIntersectSimplexConstraint.LEFT | " : "";
            result += (forbDir & BOTTOM) != 0 ? "DoNotIntersectSimplexConstraint.BOTTOM | " : "";
            result += (forbDir & RIGHT) != 0 ? "DoNotIntersectSimplexConstraint.RIGHT | " : "";
            result = result.substring(0, result.length() - 3) + ")";
            return result;
        }
    }
}
