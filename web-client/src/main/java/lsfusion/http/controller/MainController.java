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
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.authentication.LSFClientRegistrationRepository;
import lsfusion.http.authentication.LSFLoginUrlAuthenticationEntryPoint;
import lsfusion.http.authentication.LSFRemoteAuthenticationProvider;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import org.apache.http.entity.ContentType;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lsfusion.interop.session.ExternalUtils.getCharsetFromContentType;

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
    private static final String externalResourcesParentPath = "static/web/";

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String processLogin(ModelMap model, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            if (auth instanceof LSFAuthenticationToken && ((LSFAuthenticationToken) auth).isAnonymous()) {
                LSFLoginUrlAuthenticationEntryPoint.requestCache.saveRequest(request);
            } else {
                return getRedirectUrl("/main", null, request); // to prevent LSFAuthenticationSuccessHandler from showing login form twice (request cache)
            }
        }
        ServerSettings serverSettings = getAndCheckServerSettings(request, checkVersionError, false);

        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsLogo", getLogicsLogo(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));
        model.addAttribute("registrationPage", getDirectUrl("/registration", null, request));
        model.addAttribute("forgotPasswordPage", getDirectUrl("/forgot-password", null, request));

        try {
            clientRegistrationRepository.iterator().forEachRemaining(registration -> oauth2AuthenticationUrls.put(registration.getRegistrationId(),
                    getDirectUrl(authorizationRequestBaseUri + registration.getRegistrationId(), null, request)));
            model.addAttribute("urls", oauth2AuthenticationUrls);
        } catch (AuthenticationException e){
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", e);
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION_HEADER", "oauthException");
        }

        model.addAttribute("jnlpUrls", getJNLPUrls(request));
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

        JSONObject jsonResponse = sendRequest(user, request, "Authentication.registerUser");
        if (jsonResponse.has("success")){
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetails(request));
            Authentication authentication = authenticationProvider.authenticate(usernamePasswordAuthenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (jsonResponse.has("error")) {
            request.getSession(true).setAttribute("REGISTRATION_EXCEPTION", new AuthenticationException(jsonResponse.optString("error")));
            return getRedirectUrl("/registration", null, request);
        }
        return getRedirectUrl("/login", null, request);
    }

    @RequestMapping(value = "/forgot-password", method = RequestMethod.GET)
    public String forgotPassword(ModelMap model, HttpServletRequest request) {
        addStandardModelAttributes(model, request);
        return "forgot-password";
    }

    @RequestMapping(value = "/forgot-password", method = RequestMethod.POST)
    public String processForgotPassword(@RequestParam String usernameOrEmail, HttpServletRequest request) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userNameOrEmail", usernameOrEmail);
        jsonArray.put(jsonObject);

        JSONObject jsonResponse = sendRequest(jsonArray, request, "Authentication.resetPassword");
        if (jsonResponse.has("success")){
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", jsonResponse.optString("success")
                    + " " + jsonResponse.optString("email"));
        } else if (jsonResponse.has("error")) {
            String[] error = jsonResponse.optString("error").split(":");
            request.getSession(true).setAttribute("RESET_PASSWORD_EXCEPTION", error.length > 1 ? error[1] : error[0]);
            return getRedirectUrl("/forgot-password", null, request);
        }
        return getRedirectUrl("/login", null, request);
    }

    @RequestMapping(value = "/change-password", method = RequestMethod.GET)
    public String changePassword(ModelMap model, HttpServletRequest request) {
        addStandardModelAttributes(model, request);
        return "change-password";
    }

    @RequestMapping(value = "/change-password", method = RequestMethod.POST)
    public String processChangePassword(HttpServletRequest request, @RequestParam String newPassword,
                                        @RequestParam String token) {
        JSONArray user = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("newPassword", newPassword);
        jsonObject.put("token", token);
        user.put(jsonObject);

        JSONObject jsonResponse = sendRequest(user, request, "Authentication.changePassword");
        if (jsonResponse.has("success")){
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", jsonResponse.optString("success"));
        } else if (jsonResponse.has("error")) {
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", jsonResponse.optString("error"));
            return getRedirectUrl("/change-password", "token", request);
        }
        return getRedirectUrl("/login", "token", request);
    }

    private JSONObject sendRequest(JSONArray jsonArray, HttpServletRequest request, String method){
        FileData fileData = new FileData(new RawFileData(jsonArray.toString().getBytes(StandardCharsets.UTF_8)), "json");
        try {
            ExternalResponse externalResponse = logicsProvider.runRequest(request,
                    sessionObject -> sessionObject.remoteLogics.exec(AuthenticationToken.ANONYMOUS, NavigatorProviderImpl.getSessionInfo(request),
                    method + "[JSONFILE]", getExternalRequest(new Object[]{fileData}, request)));
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
        model.addAttribute("loginPage", getDirectUrl("/login",null, request));
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
        serverSettings.saveFiles(FileUtils.APP_PATH, externalResourcesParentPath);

        model.addAttribute("filesUrls", serverSettings.getFilesUrls());
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

    private String getJNLPUrls(HttpServletRequest request) {
        return "<a href=" + getDirectUrl("/exec", "action=Security.generateJnlp", request)
                + ">" + ServerMessages.getString(request, "run.desktop.client") + "</a>";
    }

    private String getFileUrl(RawFileData file) {
        return GwtSharedUtils.getDownloadURL(FileUtils.saveApplicationFile(file), null, null, false);
    }

    public static ExternalRequest getExternalRequest(Object[] params, HttpServletRequest request){
        String contentTypeString = request.getContentType();
        Charset charset = getCharsetFromContentType(contentTypeString != null ? ContentType.parse(contentTypeString) : null);
        return new ExternalRequest(new String[0], params, charset == null ? null : charset.toString(), new String[0], new String[0], null,
                null, null, null, null, request.getScheme(), request.getServerName(), request.getServerPort(), request.getContextPath(),
                request.getServletPath(), request.getQueryString() != null ? request.getQueryString() : "");
    }

    public static String getURLPreservingParameters(String url, String paramToRemove, HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (paramToRemove != null){
            List<String> params = Arrays.asList(queryString.split("&"));
            String paramString = params.stream().filter(s -> !s.contains(paramToRemove)).collect(Collectors.joining("&"));
            return !paramString.isEmpty() ? url + "?" + paramString : url;
        } else {
            queryString = !BaseUtils.isRedundantString(queryString) ? "?" + queryString : "";
            return url + queryString;
        }
    }

    public static String getDirectUrl(String url, String query, HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String queryString = request.getQueryString();
        if (query == null) {
            return queryString == null ? contextPath + url : contextPath + url + "?" + queryString;
        } else {
            return queryString == null ? contextPath + url + "?" + query : contextPath + url + "?" + queryString + "&" + query;
        }
    }

    public static String getRedirectUrl(String url, String paramToRemove, HttpServletRequest request) {
        return "redirect:" + getURLPreservingParameters(url, paramToRemove, request);
    }
}