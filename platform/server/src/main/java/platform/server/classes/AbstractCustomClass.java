package platform.server.classes;

import platform.server.classes.sets.ConcreteCustomClassSet;

import java.util.ArrayList;
import java.util.List;

public class AbstractCustomClass extends CustomClass {

    public AbstractCustomClass(String caption, CustomClass... parents) {
        super(caption, parents);
    }

    public void fillNextConcreteChilds(ConcreteCustomClassSet classSet) {
        for(CustomClass child : children)
            child.fillNextConcreteChilds(classSet);        
    }

    public static ConcreteCustomClass getSingleClass(CustomClass[] children) {
        ConcreteCustomClass single = null;
        for(CustomClass child : children) {
            ConcreteCustomClass childSingle = child.getSingleClass();
            if(childSingle==null) return null; // если несколько single то возвращаем null
            if(single==null)
                single = childSingle;
            else
                if(!childSingle.equals(single)) return null; // если другой single то
        }
        return single;
    }

    public ConcreteCustomClass getSingleClass() {
        return getSingleClass(children.toArray(new CustomClass[children.size()]));
    }

    public List<ConcreteCustomClass> getClasses() {
        return new ArrayList<ConcreteCustomClass>();
    }
}
