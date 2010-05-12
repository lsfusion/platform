package platform.server.view.navigator;

import platform.server.view.navigator.filter.CompareValueNavigator;
import platform.server.view.form.filter.CompareValue;
import platform.server.view.form.PropertyObjectInterface;

import java.sql.SQLException;

public interface ControlInterfaceNavigator extends CompareValueNavigator {

    PropertyObjectInterface doMapping(Mapper mapper);
}
