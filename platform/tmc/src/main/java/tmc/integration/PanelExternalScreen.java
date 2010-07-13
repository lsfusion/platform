package tmc.integration;

import com.jacob.activeX.ActiveXComponent;
import platform.base.BaseUtils;
import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenComponent;

import java.util.*;

public class PanelExternalScreen implements ExternalScreen {

    public int getID() {
        return 0;
    }

    public void initialize() {
    }

    // пока игнорируем все Exception'ы, чтобы лишний раз не травмировать пользователя
    public void repaint(Map<ExternalScreenComponent, SimplexConstraints> components) {

        String commPort = System.getProperty("tmc.integration.panelexternalscreen.commport");
        if (commPort == null) return;

        Map<Integer, ExternalScreenComponent> sortComps = new HashMap<Integer, ExternalScreenComponent>();
        List<Integer> sortKeys = new ArrayList<Integer>();
        for (Map.Entry<ExternalScreenComponent, SimplexConstraints> entry : components.entrySet()) {
            sortComps.put(entry.getValue().order, entry.getKey());
            sortKeys.add(entry.getValue().order);
        }

        Collections.sort(sortKeys);

        List<ExternalScreenComponent> comps = new ArrayList<ExternalScreenComponent>();
        for (Integer order : sortKeys)
            comps.add(sortComps.get(order));

        if (comps.isEmpty()) return;

        String output1 = comps.get(0).getValue();
        String output2 = comps.size() > 1 ? comps.get(1).getValue() : null;
        String output = BaseUtils.padLeft(output1 , 20) + BaseUtils.padLeft(output2 != null ? output2 : "", 20);

        ActiveXComponent commActive = null;

        try {

            if (components.keySet().iterator().next().getValue() != null) {
                commActive = new ActiveXComponent("MSCommLib.MSComm");
                commActive.setProperty("CommPort", Integer.parseInt(commPort));
                commActive.setProperty("PortOpen", true);
                commActive.setProperty("Output", new String(output.getBytes("Cp866"), "Cp1251"));
                commActive.setProperty("PortOpen", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (commActive != null) {
                try {
                    if (commActive.getPropertyAsBoolean("PortOpen"))
                        commActive.setProperty("PortOpen", false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}