package lsfusion.server.logics.navigator;

// an element or a window, which a navigator's copy has a copy of : made through the mapping (NavigatorMapping), as an
// object of a form is copied through ObjectMapping (MappingInterface)
public interface NavigatorMappingInterface<M> {
    M copy(NavigatorMapping mapping);
}
