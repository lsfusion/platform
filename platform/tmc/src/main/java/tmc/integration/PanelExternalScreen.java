package tmc.integration;

import com.jacob.activeX.ActiveXComponent;
import platform.base.BaseUtils;
import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenComponent;
import platform.interop.form.screen.ExternalScreenConstraints;
import platform.interop.form.screen.ExternalScreenParameters;

import java.util.*;

public class PanelExternalScreen implements ExternalScreen {
    private PanelExternalScreenParameters parameters = new PanelExternalScreenParameters(null);

    public int getID() {
        return 0;
    }

    public void initialize(ExternalScreenParameters parameters) {
        if (parameters instanceof PanelExternalScreenParameters) {
            this.parameters = (PanelExternalScreenParameters) parameters;
        }
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
        return a!=null ? a : "";
    }

    // пока игнорируем все Exception'ы, чтобы лишний раз не травмировать пользователя
    public void repaint(Map<ExternalScreenComponent, ExternalScreenConstraints> components) {
        String commPort = parameters.getComPort();
        if (commPort == null) {
            return;
        }

        String out[] = new String[5];
        for (Map.Entry<ExternalScreenComponent, ExternalScreenConstraints> entry : components.entrySet()) {
            out[entry.getValue().order] = entry.getKey().getValue();
        }

        String output = format(check(out[1]), check(out[2])) + format(check(out[3]), check(out[4]));
//        System.out.println(output);

        if (output.trim().equals("")){
            return;
        }
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