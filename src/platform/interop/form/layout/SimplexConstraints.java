package platform.interop.form.layout;

import java.awt.*;
import java.util.HashMap;
import java.io.Serializable;

public class SimplexConstraints extends HashMap<Component, DoNotIntersectSimplexConstraint>
                         implements Serializable {

    public int order = 0;

    public static final SimplexConstraints DEFAULT_CONSTRAINT = new SimplexConstraints();

    public DoNotIntersectSimplexConstraint childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
    public int maxVariables = 3;

//    public static int MAXIMUM = 1;
//    public static int PREFERRED = 0;

    public double fillVertical = 0; //PREFERRED;
    public double fillHorizontal = 0; //PREFERRED;

    public Insets insetsSibling = new Insets(0,0,0,0);

    //приходится ставить хотя бы один вниз, иначе криво отрисовывает объекты снизу
    public Insets insetsInside = new Insets(1,0,1,0);

    public SimplexComponentDirections directions = new SimplexComponentDirections(0.01,0.01,0,0);

    public SimplexConstraints() {
    }

}
