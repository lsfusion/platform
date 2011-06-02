package tmc.integration;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenComponent;
import platform.interop.form.screen.ExternalScreenConstraints;
import platform.interop.form.screen.ExternalScreenParameters;
import tmc.integration.exp.FiscalRegister.FiscalReg;

import java.util.Map;

public class PanelExternalScreen implements ExternalScreen {
    private final static Logger logger = Logger.getLogger(PanelExternalScreen.class);
    private PanelExternalScreenParameters parameters = new PanelExternalScreenParameters(-1, -1);

    public int getID() {
        return 0;
    }

    public void initialize(ExternalScreenParameters parameters) {
        if (parameters instanceof PanelExternalScreenParameters) {
            this.parameters = (PanelExternalScreenParameters) parameters;
        }
    }

    private String format(String a, String b, int size) {
        String result;
        if (b.contains(".")) {
            b = b.substring(0, b.indexOf('.'));
        }
        if (a.length() + b.length() > size - 1) {
            result = a.substring(0, size - 1 - b.length()) + "." + b;
        } else {
            result = a + BaseUtils.padLeft(b, size - a.length());
        }
        return result;
    }

    private String check(String a) {
        return a != null ? a : "";
    }

    // пока игнорируем все Exception'ы, чтобы лишний раз не травмировать пользователя
    public void repaint(Map<ExternalScreenComponent, ExternalScreenConstraints> components) {
        int comPort = parameters.getScreenComPort();
        int fiscalCom = parameters.getFiscalComPort();
        int size = 20;
        if (comPort < 0) {
            if (fiscalCom < 0) {
                return;
            }
            size = 26;
        }

        String out[] = new String[5];
        for (Map.Entry<ExternalScreenComponent, ExternalScreenConstraints> entry : components.entrySet()) {
            out[entry.getValue().order] = entry.getKey().getValue();
        }

        String output = format(check(out[1]), check(out[2]), size) + format(check(out[3]), check(out[4]), size);
//        System.out.println(output);


        ActiveXComponent commActive = null;
        if (comPort > 0) {
            try {
                logger.info("Before creating ActiveX");
                commActive = new ActiveXComponent("MSCommLib.MSComm");
                commActive.setProperty("CommPort", comPort);
                commActive.setProperty("PortOpen", true);
                commActive.setProperty("Output", new String(output.getBytes("Cp866"), "Cp1251"));
                commActive.setProperty("PortOpen", false);
                logger.info("After ActiveX work");
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
        } else {
            if (fiscalCom <= 0) {
                return;
            }
            if (comPort == 0) {
                return;
            }
            try {
                Dispatch cashDispatch = FiscalReg.getDispatch(fiscalCom);
                Dispatch.call(cashDispatch, "ShowDisplay", output, true, true);
                if (parameters.dispose) {
                    FiscalReg.dispose("ShowDisplay");
                }
            } catch (Exception e) {
                // пока игнорируем
            }
        }

    }
}