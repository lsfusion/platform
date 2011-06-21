package platform.server.classes;

import platform.server.classes.sets.ConcreteCustomClassSet;

public class AbstractCustomClass extends CustomClass {

    public AbstractCustomClass(String sID, String caption, CustomClass... parents) {
        super(sID, caption, parents);
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

}
