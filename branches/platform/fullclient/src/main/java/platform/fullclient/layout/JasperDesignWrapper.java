package platform.fullclient.layout;

import net.sf.jasperreports.engine.design.JasperDesign;

public class JasperDesignWrapper {
    private JasperDesign jasperDesign;
    private String caption;

    public JasperDesignWrapper() {
    }

    public JasperDesignWrapper(JasperDesign jasperDesign, String caption) {
        this.jasperDesign = jasperDesign;
        this.caption = caption;
    }

    public JasperDesign getJasperDesign() {
        return jasperDesign;
    }

    public void setJasperDesign(JasperDesign jasperDesign) {
        this.jasperDesign = jasperDesign;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
