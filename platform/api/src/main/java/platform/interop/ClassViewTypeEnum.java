package platform.interop;

import java.util.ArrayList;
import java.util.List;

public enum ClassViewTypeEnum {
    Panel, Grid, Hide;

    public static List typeNameList(){
        List list = new ArrayList();
        for(int i=0; i < ClassViewTypeEnum.values().length; i++){
            list.add(ClassViewTypeEnum.values()[i].toString());
        }
        return list;
    }
}
