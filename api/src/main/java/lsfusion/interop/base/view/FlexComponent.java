package lsfusion.interop.base.view;

import java.awt.*;

public interface FlexComponent {

    Dimension getFlexPreferredSize(Boolean vertical);
    FlexConstraints getFlexConstraints();
    boolean isShrink();
}
