package lsfusion.interop.form.layout;

import java.io.Serializable;

abstract public class SingleSimplexConstraint implements Serializable {

    public static final DoNotIntersectSimplexConstraint DO_NOT_INTERSECT = new DoNotIntersectSimplexConstraint();
    public static final IsInsideSimplexConstraint IS_INSIDE = new IsInsideSimplexConstraint();

    public static final DoNotIntersectSimplexConstraint TOTHE_RIGHTBOTTOM = new DoNotIntersectSimplexConstraint(
                                                                   DoNotIntersectSimplexConstraint.LEFT |
                                                                   DoNotIntersectSimplexConstraint.TOP);

    public static final DoNotIntersectSimplexConstraint TOTHE_LEFT = new DoNotIntersectSimplexConstraint(
                                                               DoNotIntersectSimplexConstraint.RIGHT |
                                                               DoNotIntersectSimplexConstraint.TOP |
                                                               DoNotIntersectSimplexConstraint.BOTTOM);

    public static final DoNotIntersectSimplexConstraint TOTHE_RIGHT = new DoNotIntersectSimplexConstraint(
                                                               DoNotIntersectSimplexConstraint.LEFT |
                                                               DoNotIntersectSimplexConstraint.TOP |
                                                               DoNotIntersectSimplexConstraint.BOTTOM);

    public static final DoNotIntersectSimplexConstraint TOTHE_BOTTOM = new DoNotIntersectSimplexConstraint(
                                                                   DoNotIntersectSimplexConstraint.LEFT |
                                                                   DoNotIntersectSimplexConstraint.RIGHT |
                                                                   DoNotIntersectSimplexConstraint.TOP);

    public final static int MAXVALUE = 1000000;

//    public abstract void fillConstraint(LpSolve solver, SimplexComponentInfo comp1, SimplexComponentInfo comp2) throws LpSolveException;
}
