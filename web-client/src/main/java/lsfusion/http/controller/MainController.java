package lsfusion.http.controller;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.ServerMessages;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.server.FileUtils;
import lsfusion.http.authentication.LSFAuthenticationFailureHandler;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.authentication.LSFClientRegistrationRepository;
import lsfusion.http.authentication.LSFRemoteAuthenticationProvider;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MainController {

    @Autowired
    private LogicsProvider logicsProvider;

    @Autowired
    private LSFRemoteAuthenticationProvider authenticationProvider;

    @Autowired
    private LSFClientRegistrationRepository clientRegistrationRepository;

    private final Map<String, String> oauth2AuthenticationUrls = new HashMap<>();
    private static final String authorizationRequestBaseUri = "/oauth2/authorization/";
    private final Result<String> checkVersionError = new Result<>();

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String processLogin(ModelMap model, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof LSFAuthenticationToken && ((LSFAuthenticationToken) auth).isAnonymous())) {
            return "redirect:" + LSFAuthenticationFailureHandler.getURLPreservingParameters("/main", request); // to prevent LSFAuthenticationSuccessHandler from showing login form twice (request cache)
        }
        ServerSettings serverSettings = getAndCheckServerSettings(request, checkVersionError, false);

        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsLogo", getLogicsLogo(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));
        model.addAttribute("registrationPage", "/registration");
        model.addAttribute("forgotPasswordPage", "/forgot-password");

        try {
            clientRegistrationRepository.iterator().forEachRemaining(registration -> oauth2AuthenticationUrls.put(registration.getRegistrationId(), authorizationRequestBaseUri + registration.getRegistrationId()));
            model.addAttribute("urls", oauth2AuthenticationUrls);
        } catch (AuthenticationException e){
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", e);
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION_HEADER", "oauthException");
        }

        model.addAttribute("jnlpUrls", getJNLPUrls(request, serverSettings));
        if (checkVersionError.result != null) {
            model.addAttribute("error", checkVersionError.result);
            return "restricted";
        } else {
            return "login";
        }
    }

    @RequestMapping(value = "/registration", method = RequestMethod.GET)
    public String registration(ModelMap model, HttpServletRequest request) {
        addStandardModelAttributes(model, request);
        return "registration";
    }

    @RequestMapping(value = "/registration", method = RequestMethod.POST)
    public String processRegistration(HttpServletRequest request, @RequestParam String username, @RequestParam String password,
                          @RequestParam String firstName, @RequestParam String lastName, @RequestParam String email) {
        JSONArray user = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("login", username);
        jsonObject.put("password", password);
        jsonObject.put("firstName", firstName);
        jsonObject.put("lastName", lastName);
        jsonObject.put("email", email);
        user.put(jsonObject);

        JSONObject jsonResponse = sendRequest(user, request, "registerUser");
        if (jsonResponse.has("error")) {
            request.getSession(true).setAttribute("REGISTRATION_EXCEPTION", new AuthenticationException(jsonResponse.optString("error")));
            return "redirect:/registration";
        }
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetails(request));
        Authentication authentication = authenticationProvider.authenticate(usernamePasswordAuthenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return "redirect:/login";
    }

    @RequestMapping(value = "/forgot-password", method = RequestMethod.GET)
    public String forgotPassword(ModelMap model, HttpServletRequest request) {
        addStandardModelAttributes(model, request);
        return "forgot-password";
    }

    @RequestMapping(value = "/forgot-password", method = RequestMethod.POST)
    public String processForgotPassword(@RequestParam String usernameOrEmail, HttpServletRequest request) {
        String url = "http://" + request.getLocalAddr() + ":" + request.getServerPort() + "/change-password?token=";
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userNameOrEmail", usernameOrEmail);
        jsonObject.put("url", url);
        jsonArray.put(jsonObject);

        JSONObject jsonResponse = sendRequest(jsonArray, request, "resetPassword");
        if (jsonResponse.has("error")) {
            request.getSession(true).setAttribute("RESET_PASSWORD_EXCEPTION", jsonResponse.optString("error"));
            return "redirect:/forgot-password";
        } else if (jsonResponse.has("success")) {
            request.getSession(true).setAttribute("LAST_MESSAGE", jsonResponse.optString("success"));
        }
        return "redirect:/login";
    }

    @RequestMapping(value = "/change-password", method = RequestMethod.GET)
    public String changePassword(ModelMap model, HttpServletRequest request, @RequestParam String token) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", token);
        jsonArray.put(jsonObject);

        JSONObject jsonResponse = sendRequest(jsonArray, request, "checkToken");
        if (jsonResponse.has("error")) {
            request.getSession(true).setAttribute("LAST_MESSAGE", jsonResponse.optString("error"));
        } else if (jsonResponse.has("success")){
            addStandardModelAttributes(model, request);
            return "change-password";
        }
        return "redirect:/login";
    }

    @RequestMapping(value = "/change-password", method = RequestMethod.POST)
    public String processChangePassword(HttpServletRequest request, @RequestParam String newPassword,
                                        @RequestParam String token) {
        JSONArray user = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("newPassword", newPassword);
        jsonObject.put("token", token);
        user.put(jsonObject);

        JSONObject jsonResponse = sendRequest(user, request, "changePassword");
        if (jsonResponse.has("error")) {
            request.getSession(true).setAttribute("LAST_MESSAGE", jsonResponse.optString("error"));
        } else if (jsonResponse.has("success")){
            request.getSession(true).setAttribute("LAST_MESSAGE", jsonResponse.optString("success"));
        }
        return "redirect:/login";
    }

    private JSONObject sendRequest(JSONArray jsonArray, HttpServletRequest request, String method){
        FileData fileData = new FileData(new RawFileData(jsonArray.toString().getBytes(StandardCharsets.UTF_8)), "json");
        try {
            ExternalResponse externalResponse = logicsProvider.runRequest(request, new LogicsRunnable<ExternalResponse>() {
                @Override
                public ExternalResponse run(LogicsSessionObject sessionObject) throws RemoteException {
                    return sessionObject.remoteLogics.exec(AuthenticationToken.ANONYMOUS, NavigatorProviderImpl.getSessionInfo(request),
                            "Authentication." + method + "[JSONFILE]", new ExternalRequest(new Object[]{fileData}));
                }
            });
            return new JSONObject(new String(((FileData) externalResponse.results[0]).getRawFile().getBytes(), StandardCharsets.UTF_8));
        } catch (IOException | AppServerNotAvailableDispatchException e) {
            throw Throwables.propagate(e);
        }
    }

    private void addStandardModelAttributes(ModelMap model, HttpServletRequest request) {
        ServerSettings serverSettings = getAndCheckServerSettings(request, checkVersionError, false);
        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsLogo", getLogicsLogo(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));
        model.addAttribute("loginPage", "/login");
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