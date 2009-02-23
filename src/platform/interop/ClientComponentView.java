package platform.interop;

import java.io.Serializable;

import platform.client.form.layout.SimplexConstraints;

public class ClientComponentView implements Serializable {

    public ClientContainerView container;
    public SimplexConstraints constraints = new SimplexConstraints();

    public String outName = "";

}
