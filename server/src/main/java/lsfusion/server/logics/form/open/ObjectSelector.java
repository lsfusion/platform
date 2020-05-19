package lsfusion.server.logics.form.open;

import lsfusion.server.data.type.Type;

// for form polymorphism
public interface ObjectSelector {
    
    boolean noClasses();

    Type getType();
}
