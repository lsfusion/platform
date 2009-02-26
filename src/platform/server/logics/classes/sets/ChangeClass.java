package platform.server.logics.classes.sets;

import platform.server.logics.properties.PropertyInterface;

import java.util.Map;

// по сути Entry для ValueClassSet'а
public class ChangeClass<P extends PropertyInterface> {

    public InterfaceClassSet<P> interfaceClasses;
    public ClassSet value;

    ChangeClass() {
        interfaceClasses = new InterfaceClassSet<P>();
        value = new ClassSet();
    }

    public ChangeClass(InterfaceClassSet<P> iInterface, ClassSet iValue) {
        interfaceClasses = iInterface;
        value = iValue;
    }

    public ChangeClass(InterfaceClass<P> iInterface, ClassSet iValue) {
        interfaceClasses = new InterfaceClassSet<P>(iInterface);
        value = iValue;
    }

    public ChangeClass(P iInterface, ClassSet iInterfaceValue) {
        interfaceClasses = new InterfaceClassSet<P>(new InterfaceClass<P>(iInterface,iInterfaceValue));
        value = new ClassSet();
    }

    /*    ChangeClass(InterfaceClassSet<P> iInterface) {
          Interface = iInterface;
          Value = new OrClassSet();
      }

      public String toString() {
          return Interface.toString() + " - V - " + Value.toString();
      }

      public ChangeClass(ClassSet iValue) {
          Value = iValue;
          Interface = new InterfaceClassSet<P>();
      }

      public ChangeClass(P iInterface, Class Class) {
          Interface = new InterfaceClassSet<P>(iInterface,Class);
          Value = new OrClassSet();
      }

      public ChangeClass(Class valueClass) {
          Value = new OrClassSet(valueClass);
          Interface = new InterfaceClassSet<P>();
      }
    */
    <V extends PropertyInterface> ChangeClass<V> map(Map<P,V> mapInterfaces) {
        return new ChangeClass<V>(interfaceClasses.map(mapInterfaces), value);
    }

    <V extends PropertyInterface> ChangeClass<V> mapBack(Map<V,P> mapInterfaces) {
        return new ChangeClass<V>(interfaceClasses.mapBack(mapInterfaces), value);
    }

    public String toString() {
        return interfaceClasses.toString() + " - " + value.toString();
    }
}
