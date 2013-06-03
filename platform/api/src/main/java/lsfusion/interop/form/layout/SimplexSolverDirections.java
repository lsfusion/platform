package lsfusion.interop.form.layout;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import java.util.ArrayList;
import java.util.List;

public class SimplexSolverDirections {

    Integer L;
    Integer R;
    Integer T;
    Integer B;

    public SimplexSolverDirections(LpSolve solver, int forbDir) throws LpSolveException {

        int startColumn = solver.getNcolumns();

        int varCount = 0;
        List<Integer> varList = new ArrayList<Integer>();

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

//        if (varCount > 1)
//            System.out.println("LPSolve : addVariables - " + varCount);

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
