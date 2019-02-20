package lsfusion.http.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.RawFileData;
import lsfusion.base.ServerMessages;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.logics.ServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

@Controller
public class MainController {

    @Autowired
    LogicsProvider logicsProvider;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String processLogin(ModelMap model, HttpServletRequest request) {
        ServerSettings serverSettings = getServerSettings(request);

        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsLogo", getLogicsLogo(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));

        String error = checkApiVersion(request, serverSettings);
        if (error != null) {
            model.addAttribute("error", error);
            return "restricted";
        } else {
            return "login";
        }
    }

    @RequestMapping(value = "/main", method = RequestMethod.GET)
    public String processMain(ModelMap model, HttpServletRequest request) {
        ServerSettings serverSettings = getServerSettings(request);

        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));

        return "main";
    }

    private ServerSettings getServerSettings(HttpServletRequest request) {
        try {
            return logicsProvider.getServerSettings(request);
        } catch (Exception e) {
            return null;
        }
    }

    private String getTitle(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.displayName != null ? serverSettings.displayName : "lsfusion";
    }

    private String getLogicsLogo(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.logicsLogo != null ? getFileUrl(serverSettings.logicsLogo) : "static/images/logo.png";
    }

    private String getLogicsIcon(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.logicsIcon != null ? getFileUrl(serverSettings.logicsIcon) : "favicon.ico";
    }

    private String getFileUrl(RawFileData file) {
        return GwtSharedUtils.getDownloadURL(FileUtils.saveApplicationFile(file), null, null, false);
    }

    private String checkApiVersion(HttpServletRequest request, ServerSettings serverSettings) {
        String result = null;
        if (serverSettings != null) {
            String serverVersion = null;
            String clientVersion = null;
            String clientPlatformVersion = BaseUtils.getPlatformVersion();
            if (clientPlatformVersion == null || !clientPlatformVersion.equals(serverSettings.platformVersion)) {
                serverVersion = serverSettings.platformVersion;
                clientVersion = clientPlatformVersion;
            } else {
                Integer clientApiVersion = BaseUtils.getApiVersion();
                if (!clientApiVersion.equals(serverSettings.apiVersion)) {
                    serverVersion = serverSettings.platformVersion + " [" + serverSettings.apiVersion + "]";
                    clientVersion = clientPlatformVersion + " [" + clientApiVersion + "]";
                }
            }
            result = serverVersion != null ? ServerMessages.getString(request, "check.api.version", serverVersion, clientVersion) : null;
        }
        return result;
    }
}