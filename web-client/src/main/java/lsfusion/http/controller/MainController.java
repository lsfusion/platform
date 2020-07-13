package lsfusion.http.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.ServerMessages;
import lsfusion.base.file.RawFileData;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.server.FileUtils;
import lsfusion.http.authentication.LSFAuthenticationFailureHandler;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.authentication.LSFClientRegistrationRepository;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.logics.ServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MainController {

    @Autowired
    LogicsProvider logicsProvider;

    @Autowired
    private LSFClientRegistrationRepository clientRegistrationRepository;

    private final Map<String, String> oauth2AuthenticationUrls = new HashMap<>();
    private static final String authorizationRequestBaseUri = "/oauth2/authorization/";

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String processLogin(ModelMap model, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof LSFAuthenticationToken && ((LSFAuthenticationToken) auth).isAnonymous())) {
            return "redirect:" + LSFAuthenticationFailureHandler.getURLPreservingParameters("/main", request); // to prevent LSFAuthenticationSuccessHandler from showing login form twice (request cache)
        }

        Result<String> checkVersionError = new Result<>();
        ServerSettings serverSettings = getAndCheckServerSettings(request, checkVersionError, false);

        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsLogo", getLogicsLogo(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));

        Iterable<ClientRegistration> clientRegistrations = clientRegistrationRepository;
        clientRegistrations.forEach(registration -> oauth2AuthenticationUrls.put(registration.getClientName(), authorizationRequestBaseUri + registration.getRegistrationId()));
        model.addAttribute("urls", oauth2AuthenticationUrls);

        model.addAttribute("jnlpUrls", getJNLPUrls(request, serverSettings));
        if (checkVersionError.result != null) {
            model.addAttribute("error", checkVersionError.result);
            return "restricted";
        } else {
            return "login";
        }
    }

    private ServerSettings getAndCheckServerSettings(HttpServletRequest request, Result<String> rCheck, boolean noCache) {
        ServerSettings serverSettings = getServerSettings(request, noCache);
        String checkVersionError = serverSettings != null ? BaseUtils.checkClientVersion(serverSettings.platformVersion, serverSettings.apiVersion, BaseUtils.getPlatformVersion(),  BaseUtils.getApiVersion()) : null;
        if(checkVersionError != null) {
            if(!noCache) // try without cache
                return getAndCheckServerSettings(request, rCheck, true);
            rCheck.set(checkVersionError);
        }
        return serverSettings;
    }

    @RequestMapping(value = "/main", method = RequestMethod.GET)
    public String processMain(ModelMap model, HttpServletRequest request) {
        ServerSettings serverSettings = getServerSettings(request, false);

        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));
        model.addAttribute("logicsName", getLogicsName(serverSettings));

        return "main";
    }

    private ServerSettings getServerSettings(HttpServletRequest request, boolean noCache) {
        return logicsProvider.getServerSettings(request, noCache);
    }

    private String getTitle(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.displayName != null ? serverSettings.displayName : "lsfusion";
    }

    private String getLogicsLogo(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.logicsLogo != null ? getFileUrl(serverSettings.logicsLogo) : "static/noauth/images/logo.png";
    }

    private String getLogicsIcon(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.logicsIcon != null ? getFileUrl(serverSettings.logicsIcon) : "favicon.ico";
    }
    
    private String getLogicsName(ServerSettings serverSettings) {
        return serverSettings != null ? serverSettings.logicsName : null;
    }

    private String getJNLPUrls(HttpServletRequest request, ServerSettings serverSettings) {
        return serverSettings != null ? serverSettings.jnlpUrls : ("<a href=" + request.getContextPath() + "/exec?action=Security.generateJnlp>" + ServerMessages.getString(request, "run.desktop.client") + "</a>");
    }

    private String getFileUrl(RawFileData file) {
        return GwtSharedUtils.getDownloadURL(FileUtils.saveApplicationFile(file), null, null, false);
    }
}