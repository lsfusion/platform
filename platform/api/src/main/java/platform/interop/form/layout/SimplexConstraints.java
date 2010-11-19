package platform.interop.form.layout;

import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SimplexConstraints<T> implements Serializable {

    public static final SimplexConstraints DEFAULT_CONSTRAINT = new SimplexConstraints();

    public DoNotIntersectSimplexConstraint childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
    public int maxVariables = 3;

//    public static int MAXIMUM = 1;
//    public static int PREFERRED = 0;

    // значение > 0 значит, что надо растягивать с определенным коэффициентом
    // значение == 0 значит, что нужно стремиться к preferredSize
    // значение < 0 значит, что нужно быть как можно меньше
    public double fillVertical = 0; //PREFERRED;
    public double fillHorizontal = 0; //PREFERRED;

    public Insets insetsSibling = new Insets(0,0,0,0);

    //приходится ставить хотя бы один вниз, иначе криво отрисовывает объекты снизу
    public Insets insetsInside = new Insets(1,0,1,0);

    public SimplexComponentDirections directions = new SimplexComponentDirections(0.01,0.01,0,0);

    // приходится делать сериализацию отдельно, посколько клиент будет работать не с исходным классом T, а с его ID
    transient public Map<T, DoNotIntersectSimplexConstraint> intersects = new HashMap<T, DoNotIntersectSimplexConstraint>();

    public SimplexConstraints() {
    }

}
