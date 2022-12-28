package lsfusion.gwt.client;

import lsfusion.gwt.client.navigator.GPropertyNavigator;

import java.io.Serializable;

public class GNavigatorChangesDTO implements Serializable {
    public GPropertyNavigator[] properties;
    public Serializable[] values;
}