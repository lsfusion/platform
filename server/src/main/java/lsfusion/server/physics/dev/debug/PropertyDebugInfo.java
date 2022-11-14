package lsfusion.server.physics.dev.debug;

public class PropertyDebugInfo extends DebugInfo {

    public PropertyDebugInfo(DebugInfo.DebugPoint point, boolean needToCreateDelegate) {
        super(point);
        setNeedToCreateDelegate(needToCreateDelegate);
    }
}
