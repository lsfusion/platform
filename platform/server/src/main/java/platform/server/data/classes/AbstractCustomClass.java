package platform.server.data.classes;

import platform.server.data.classes.where.ConcreteCustomClassSet;

public class AbstractCustomClass extends CustomClass {

    public AbstractCustomClass(Integer iID, String iCaption, CustomClass... iParents) {
        super(iID, iCaption, iParents);
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
