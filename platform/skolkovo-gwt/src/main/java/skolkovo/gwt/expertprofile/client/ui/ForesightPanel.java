package skolkovo.gwt.expertprofile.client.ui;

import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import platform.gwt.base.client.ui.HLayout100;
import skolkovo.api.gwt.shared.ForesightInfo;
import skolkovo.api.gwt.shared.ProfileInfo;

import java.util.Stack;

public class ForesightPanel extends HLayout100 {
    private ProfileInfo pi;

    public ForesightPanel(ProfileInfo PI) {
        this.pi = PI;

        ForesightInfo[] foresightInfos = pi.foresightInfos;
        int cnt = Math.min(15, foresightInfos.length);
        ForesightRecord data[] = new ForesightRecord[cnt];
        Stack s = new Stack();
        for (int i = 0; i < cnt; i++) {
            ForesightInfo foresight = foresightInfos[i];
            data[i] = new ForesightRecord(foresight);
        }

        ListGrid grid = new ListGrid();
        grid.setWidth100();
        grid.setHeight100();
        grid.setFields(new ListGridField("name"));
        grid.setData(data);

        addMember(grid);
    }

    public static class ForesightRecord extends ListGridRecord {
        public ForesightRecord(ForesightInfo fi) {
            setAttribute("name", fi.sID + ":: " + fi.name + ":: " + fi.substantiation);
        }
    }
}
