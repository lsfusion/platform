package platform.gwt.view;

import java.util.ArrayList;

public class GContainer extends GComponent {
    public ArrayList<GComponent> children = new ArrayList<GComponent>();
    public String title;
    public String description;
    public byte type;
    public boolean gwtIsLayout;
    public boolean gwtVertical;
    public Alignment hAlign;
    public boolean resizable;

    public enum Alignment {
        LEFT, RIGHT, CENTER;

        public com.smartgwt.client.types.Alignment getSmartGWTAlignment () {
            switch (this) {
                case LEFT:
                    return com.smartgwt.client.types.Alignment.LEFT;
                case RIGHT:
                    return com.smartgwt.client.types.Alignment.RIGHT;
                case CENTER:
                    return com.smartgwt.client.types.Alignment.CENTER;
            }
            return null;
        }
    }
}
