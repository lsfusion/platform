package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;

import java.text.Format;

public interface ClientType {

    int getMinimumWidth();
    int getPreferredWidth();
    int getMaximumWidth();

    Format getDefaultFormat();

    PropertyRendererComponent getRendererComponent(Format format);    
}
