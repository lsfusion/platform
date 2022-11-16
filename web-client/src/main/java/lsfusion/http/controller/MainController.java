package lsfusion.http.controller;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.ServerMessages;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
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
import java.util.*;
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

        model.addAttribute("disableRegistration", getDisableRegistration(serverSettings));
        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsLogo", getLogicsLogo(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));
        model.addAttribute("registrationPage", getDirectUrl("/registration", null, null, request));
        model.addAttribute("forgotPasswordPage", getDirectUrl("/forgot-password", null, null, request));
        model.addAttribute("loginResources",
                serverSettings != null && serverSettings.loginResources != null ? saveResources(serverSettings, serverSettings.loginResources) : null);

        try {
            clientRegistrationRepository.iterator().forEachRemaining(registration -> oauth2AuthenticationUrls.put(registration.getRegistrationId(),
                    getDirectUrl(authorizationRequestBaseUri + registration.getRegistrationId(), null, null, request)));
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
        return getDisableRegistration(getAndCheckServerSettings(request, checkVersionError, false)) ? "login" : "registration";
    }

    @RequestMapping(value = "/registration", method = RequestMethod.POST)
    public String processRegistration(HttpServletRequest request, @RequestParam String username, @RequestParam String password,
                          @RequestParam String firstName, @RequestParam String lastName, @RequestParam String email) {

        if (getDisableRegistration(getAndCheckServerSettings(request, checkVersionError, false)))
            return "login";

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
            SecurityContextHolder.getContext().setAuthentication(getAuthentication(request, username, password, authenticationProvider));
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
            return getRedirectUrl("/change-password", jsonResponse.has("passwordStrengthError") ? null : Collections.singletonList("token"), request);
        }
        return getRedirectUrl("/login", Collections.singletonList("token"), request);
    }

    private JSONObject sendRequest(JSONArray jsonArray, HttpServletRequest request, String method){
        FileData fileData = new FileData(new RawFileData(jsonArray.toString().getBytes(StandardCharsets.UTF_8)), "json");
        try {
            ExternalResponse externalResponse = logicsProvider.runRequest(request,
                    (sessionObject, retry) -> sessionObject.remoteLogics.exec(AuthenticationToken.ANONYMOUS, NavigatorProviderImpl.getSessionInfo(request),
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
        model.addAttribute("loginPage", getDirectUrl("/login", Collections.singletonList("token"), null, request));
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
        model.addAttribute("lsfParams", getLsfParams(serverSettings));
        model.addAttribute("mainResources",
                serverSettings != null && serverSettings.mainResources != null ? saveResources(serverSettings, serverSettings.mainResources) : null);

        return "main";
    }

    private Map<String, String> saveResources(ServerSettings serverSettings, List<Pair<String, RawFileData>> resources) {
        Map<String, String> versionedResources = new LinkedHashMap<>();
        for (Pair<String, RawFileData> resource : resources) {
            String fileName = resource.first;
            versionedResources.put(FileUtils.saveWebFile(fileName, resource.second, serverSettings), fileName.substring(fileName.lastIndexOf(".") + 1));
        }
        return versionedResources;
    }

    private ServerSettings getServerSettings(HttpServletRequest request, boolean noCache) {
        return logicsProvider.getServerSettings(request, noCache);
    }

    private boolean getDisableRegistration(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.disableRegistration;
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
        String localizedString = ServerMessages.getString(request, "run.desktop.client");
        return serverSettings != null ? serverSettings.jnlpUrls.replaceAll("\\{run.desktop.client}", localizedString)
                : "<a href=" + getDirectUrl("/exec", null, "action=Security.generateJnlp", request) + ">" + localizedString + "</a>";
    }

    private Map<String, String> getLsfParams(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.lsfParams != null ? serverSettings.lsfParams : null;
    }

    private String getFileUrl(RawFileData file) {
        assert file != null;
        return FileUtils.saveApplicationFile(file);
    }

    public static ExternalRequest getExternalRequest(Object[] params, HttpServletRequest request){
        String contentTypeString = request.getContentType();
        Charset charset = getCharsetFromContentType(contentTypeString != null ? ContentType.parse(contentTypeString) : null);
        return new ExternalRequest(new String[0], params, charset.toString(), new String[0], new String[0], null,
                null, null, null, null, request.getScheme(), request.getMethod(), request.getServerName(), request.getServerPort(), request.getContextPath(),
                request.getServletPath(), request.getPathInfo() == null ? "" : request.getPathInfo(), request.getQueryString() != null ? request.getQueryString() : "",
                contentTypeString, null);
    }

    public static String getURLPreservingParameters(String url, List<String> paramsToRemove, HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (paramsToRemove != null && queryString != null){
            List<String> params = Arrays.asList(queryString.split("&"));
            String paramString = params.stream().filter(s -> (paramsToRemove.stream().noneMatch(s::contains))).collect(Collectors.joining("&"));
            return !paramString.isEmpty() ? url + "?" + paramString : url;
        } else {
            queryString = !BaseUtils.isRedundantString(queryString) ? "?" + queryString : "";
            return url + queryString;
        }
    }

    public static String getDirectUrl(String url, List<String> paramsToRemove, String query, HttpServletRequest request) {
        return request.getContextPath() + getURLPreservingParameters(url, paramsToRemove, request) +
                (query != null ? (request.getQueryString() == null ? "?" : "&") + query : "");
    }

    public static String getRedirectUrl(String url, List<String> paramsToRemove, HttpServletRequest request) {
        return "redirect:" + getURLPreservingParameters(url, paramsToRemove, request);
    }

    public static Authentication getAuthentication(HttpServletRequest request, String userName, String password, LSFRemoteAuthenticationProvider authenticationProvider) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userName, password);
        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetails(request));
        return authenticationProvider.authenticate(usernamePasswordAuthenticationToken);
    }
}