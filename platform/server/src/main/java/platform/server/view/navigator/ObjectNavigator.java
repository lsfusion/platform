package platform.server.view.navigator;

import platform.server.data.classes.ValueClass;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.view.navigator.filter.CompareValueNavigator;
import platform.server.view.form.filter.CompareValue;

import java.util.Set;

public class ObjectNavigator implements CompareValueNavigator {

    public ObjectNavigator(int iID, ValueClass iBaseClass, String iCaption) {
        ID = iID;
        caption = iCaption;
        baseClass = iBaseClass;
    }

    public GroupObjectNavigator groupTo;

    public final String caption;

    public final ValueClass baseClass;

    // идентификатор (в рамках формы)
    public final int ID;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public String sID;
    public String getSID() {
        if (sID != null) return sID; else return "obj" + ID;
    }

    public CompareValue doMapping(FilterNavigator.Mapper mapper) {
        return mapper.mapObject(this);
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
        objects.add(this);
    }    
}
