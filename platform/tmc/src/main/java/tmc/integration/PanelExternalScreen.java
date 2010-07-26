package tmc.integration;

import com.jacob.activeX.ActiveXComponent;
import platform.base.BaseUtils;
import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenComponent;
import platform.interop.form.screen.ExternalScreenConstraints;

import java.util.*;

public class PanelExternalScreen implements ExternalScreen {

    public int getID() {
        return 0;
    }

    public void initialize() {
    }

    private String format(String a, String b){
        String result;
        if (a.length()+b.length()>19) {
            result = a.substring(0, 19-b.length())+"."+b;
        } else {
            result = a + BaseUtils.padLeft(b, 20-a.length());
        }
        return result;
    }

    private String check(String a){
        return a!=null ? a:"";
    }

    // пока игнорируем все Exception'ы, чтобы лишний раз не травмировать пользователя
    public void repaint(Map<ExternalScreenComponent, ExternalScreenConstraints> components) {

        String commPort = System.getProperty("tmc.integration.panelexternalscreen.commport");
        //if (commPort == null) return;

        Map<Integer, ExternalScreenComponent> sortComps = new HashMap<Integer, ExternalScreenComponent>();
        List<Integer> sortKeys = new ArrayList<Integer>();
        for (Map.Entry<ExternalScreenComponent, ExternalScreenConstraints> entry : components.entrySet()) {
            sortComps.put(entry.getValue().order, entry.getKey());
            sortKeys.add(entry.getValue().order);
        }

        Collections.sort(sortKeys);

        List<ExternalScreenComponent> comps = new ArrayList<ExternalScreenComponent>();
        for (Integer order : sortKeys)
            comps.add(sortComps.get(order));

        if (comps.isEmpty()) return;

        String output1 = comps.size() > 0 ? comps.get(0).getValue() : "";
        String output2 = comps.size() > 1 ? comps.get(1).getValue() : "";
        String output3 = comps.size() > 2 ? comps.get(2).getValue() : "";
        String output4 = comps.size() > 3 ? comps.get(3).getValue() : "";
        //String output = BaseUtils.padLeft(output1 != null ? output1 : "", 20) + BaseUtils.padLeft(output2 != null ? output2 : "", 20);
        String output = format(check(output1), check(output2)) + format(check(output3), check(output4));
        System.out.println(output);


        if (output.trim().equals("")){
            return;
        }
        if (true) return;
        ActiveXComponent commActive = null;

        try {

            if (components.keySet().iterator().next().getValue() != null) {
                System.out.println("Before creating ActiveX");
                commActive = new ActiveXComponent("MSCommLib.MSComm");
                commActive.setProperty("CommPort", Integer.parseInt(commPort));
                commActive.setProperty("PortOpen", true);
                commActive.setProperty("Output", new String(output.getBytes("Cp866"), "Cp1251"));
                commActive.setProperty("PortOpen", false);
                System.out.println("After ActiveX work");
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