package platform.server.logics.linear;

import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.ArrayList;
import java.util.List;

public class LCP<T extends PropertyInterface> extends LP<T, CalcProperty<T>> {

    public LCP(CalcProperty<T> property) {
        super(property);
    }

    public LCP(CalcProperty<T> property, List<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public static List<CalcProperty> toPropertyArray(LCP[] properties) {
        List<CalcProperty> result = new ArrayList<CalcProperty>();
        for (LCP<?> property : properties)
            result.add(property.property);
        return result;
    }

}
