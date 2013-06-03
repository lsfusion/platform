package lsfusion.server.classes;

import lsfusion.base.col.interfaces.mutable.MSet;

public class AbstractCustomClass extends CustomClass {

    public AbstractCustomClass(String sID, String caption, CustomClass... parents) {
        super(sID, caption, parents);
    }

    public void fillNextConcreteChilds(MSet<ConcreteCustomClass> mClassSet) {
        for(CustomClass child : children)
            child.fillNextConcreteChilds(mClassSet);
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
