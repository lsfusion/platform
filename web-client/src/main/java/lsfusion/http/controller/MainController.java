package lsfusion.http.controller;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
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
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ClientType;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.navigator.ClientInfo;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
import net.customware.gwt.dispatch.shared.general.StringResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION;

@Controller
public class MainController {

    private final LogicsProvider logicsProvider;
    private final LSFRemoteAuthenticationProvider authenticationProvider;
    private final LSFClientRegistrationRepository clientRegistrationRepository;
    private final NavigatorProvider navigatorProvider;

    public MainController(LogicsProvider logicsProvider, LSFRemoteAuthenticationProvider authenticationProvider, LSFClientRegistrationRepository clientRegistrationRepository, NavigatorProvider navigatorProvider) {
        this.logicsProvider = logicsProvider;
        this.authenticationProvider = authenticationProvider;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.navigatorProvider = navigatorProvider;
    }

    private final Map<String, String> oauth2AuthenticationUrls = new HashMap<>();
    private static final String authorizationRequestBaseUri = "/oauth2/authorization/";
    private final Result<String> checkVersionError = new Result<>();

    @RequestMapping(value = "/push-notification", method = RequestMethod.GET)
    public String pushNotification(ModelMap model, HttpServletRequest request) {
        model.addAttribute("id", request.getParameter(GwtSharedUtils.NOTIFICATION_PARAM));
        model.addAttribute("query", getQueryPreservingParameters(Collections.singletonList(GwtSharedUtils.NOTIFICATION_PARAM), request));
        addStandardModelAttributes(model, request, getAndCheckServerSettings(request, checkVersionError, false), true);
        return "push-notification";
    }

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
        model.addAttribute("registrationPage", getDirectUrl("/registration", null, null, request));
        model.addAttribute("forgotPasswordPage", getDirectUrl("/forgot-password", null, null, request));
        addStandardModelAttributes(model, request, serverSettings, true);

        try {
            clientRegistrationRepository.iterator().forEachRemaining(registration -> oauth2AuthenticationUrls.put(registration.getRegistrationId(),
                    getDirectUrl(authorizationRequestBaseUri + registration.getRegistrationId(), null, null, request)));
            model.addAttribute("urls", oauth2AuthenticationUrls);
        } catch (Throwable e){
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", e);
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION_HEADER", "oauthException");
        }

        model.addAttribute("jnlpUrls", getJNLPUrls(request, serverSettings));
        addUserDataAttributes(model, request);
        if (checkVersionError.result != null) {
            model.addAttribute("error", checkVersionError.result);
            checkVersionError.set(null);
            return "restricted";
        } else {
            return "login";
        }
    }

    @GetMapping(value = "/manifest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getLSFManifest(HttpServletRequest request) {
        ServerSettings serverSettings = getAndCheckServerSettings(request, checkVersionError, false);

        Map<String, Object> response = new HashMap<>();
        response.put("name", getTitle(serverSettings));
        List<Map<String, Object>> icons = new ArrayList<>();
        Map<String, Object> iconsMap = new HashMap<>();
        iconsMap.put("src", getPWAIcon(serverSettings));
        iconsMap.put("type", "image/png");
        iconsMap.put("sizes", "512x512");
        icons.add(iconsMap);
        response.put("icons", icons);
        String contextPath = request.getContextPath();
        response.put("id", (!contextPath.isEmpty() ? contextPath + "/" : "") + "main");
        response.put("start_url", "main");
        response.put("display", "standalone");
        response.put("scope", contextPath + "/");

        return response;
    }

    @RequestMapping(value = "/registration", method = RequestMethod.GET)
    public String registration(ModelMap model, HttpServletRequest request) {
        ServerSettings serverSettings = getAndCheckServerSettings(request, checkVersionError, false);
        addStandardModelAttributes(model, request, serverSettings, true);
        addUserDataAttributes(model, request);
        return getDisableRegistration(serverSettings) ? "login" : "registration";
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
            Map<String, String> userData = new HashMap<>();
            userData.put("username", username);
            userData.put("firstName", firstName);
            userData.put("lastName", lastName);
            userData.put("email", email);
            request.getSession(true).setAttribute("USER_DATA", userData);
            return getRedirectUrl("/registration", null, request);
        }
        return getRedirectUrl("/login", null, request);
    }

    @RequestMapping(value = "/forgot-password", method = RequestMethod.GET)
    public String forgotPassword(ModelMap model, HttpServletRequest request) {
        addStandardModelAttributes(model, request, getAndCheckServerSettings(request, checkVersionError, false), true);
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
        addStandardModelAttributes(model, request, getAndCheckServerSettings(request, checkVersionError, false), true);
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

    public static ClientSettings getClientSettings(RemoteNavigatorInterface remoteNavigator, HttpServletRequest request, ClientInfo clientInfo) throws RemoteException {
        remoteNavigator.updateClientInfo(clientInfo);
        return LogicsSessionObject.getClientSettings(MainController.getExternalRequest(new ExternalRequest.Param[0], request), remoteNavigator);
    }

    private void addUserDataAttributes(ModelMap model, HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object userDataSessionAttribute = session.getAttribute("USER_DATA");
        if (userDataSessionAttribute != null) {
            Map<?, ?> userData = (Map<?, ?>) userDataSessionAttribute;
            userData.keySet().forEach(key -> model.addAttribute((String) key, userData.get(key)));
            session.removeAttribute("USER_DATA");
        }
    }

    private void addStandardModelAttributes(ModelMap model, HttpServletRequest request, ServerSettings serverSettings, boolean noAuth) {
        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsLogo", getLogicsLogo(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));
        model.addAttribute("loginPage", getDirectUrl("/login", Collections.singletonList("token"), null, request));
        model.addAttribute("apiVersion", BaseUtils.getPlatformVersion() + " (" + BaseUtils.getApiVersion() + ")");

        if (noAuth){
            model.addAttribute("noAuthResourcesBeforeSystem", getSavedNoAuthResources(serverSettings, true));
            model.addAttribute("noAuthResourcesAfterSystem", getSavedNoAuthResources(serverSettings, false));
        }
    }

    private Map<String, String> getSavedNoAuthResources(ServerSettings serverSettings, boolean before) {
        Map<String, String> savedResources = null;
        if (serverSettings != null) {
            List<Pair<String, RawFileData>> resources = before ? serverSettings.noAuthResourcesBeforeSystem : serverSettings.noAuthResourcesAfterSystem;
            if (resources != null)
                savedResources = saveResources(serverSettings, resources, true);
        }
        return savedResources;
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

        addStandardModelAttributes(model, request, serverSettings, false);
        model.addAttribute("logicsName", getLogicsName(serverSettings));
        model.addAttribute("lsfParams", getLsfParams(serverSettings));

        String sessionId;
        List<Pair<String, RawFileData>> mainResourcesBeforeSystem;
        List<Pair<String, RawFileData>> mainResourcesAfterSystem;
        try {
            sessionId = logicsProvider.runRequest(request, (sessionObject, retry) -> {
                try {
                    return new StringResult(navigatorProvider.createNavigator(sessionObject, request));
                } catch (RemoteMessageException e) {
                    request.getSession().setAttribute(AUTHENTICATION_EXCEPTION, new InternalAuthenticationServiceException(e.getMessage()));
                    throw e;
                }
            }).get();
            LogicsSessionObject.InitSettings initSettings = getInitSettings(navigatorProvider.getNavigatorSessionObject(sessionId).remoteNavigator, request, new ClientInfo("1366x768", 1.0, ClientType.WEB_DESKTOP, true));
            mainResourcesBeforeSystem = initSettings.mainResourcesBeforeSystem;
            mainResourcesAfterSystem = initSettings.mainResourcesAfterSystem;
        } catch (AuthenticationException authenticationException) {
            return getRedirectUrl("/logout", null, request);
        } catch (Throwable e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("redirectURL", getDirectUrl("/main", null, null, request));
            return "app-not-available";
        }

        model.addAttribute("sessionID", sessionId);
        model.addAttribute("mainResourcesBeforeSystem", serverSettings != null && mainResourcesBeforeSystem != null ? saveResources(serverSettings, mainResourcesBeforeSystem, false) : null);
        model.addAttribute("mainResourcesAfterSystem", serverSettings != null && mainResourcesAfterSystem != null ? saveResources(serverSettings, mainResourcesAfterSystem, false) : null);

        return "main";
    }

    private Map<String, String> saveResources(ServerSettings serverSettings, List<Pair<String, RawFileData>> resources, boolean noAuth) {
        Map<String, String> versionedResources = new LinkedHashMap<>();
        for (Pair<String, RawFileData> resource : resources) {
            String fullPath = resource.first;
            String extension = BaseUtils.getFileExtension(fullPath);
            versionedResources.put(extension.equals("html") ? resource.second.convertString() : FileUtils.saveWebFile(fullPath, resource.second, serverSettings, noAuth), extension);
        }
        return versionedResources;
    }

    private ServerSettings getServerSettings(HttpServletRequest request, boolean noCache) {
        return logicsProvider.getServerSettings(request, noCache);
    }

    private JSONObject sendRequest(JSONArray jsonArray, HttpServletRequest request, String method){
        ExternalRequest.Param fileParam = ExternalRequest.getSystemParam(jsonArray.toString());
        try {
            return logicsProvider.runRequest(request,
                    (sessionObject, retry) -> LogicsSessionObject.getJSONObjectResult(sessionObject.remoteLogics.exec(AuthenticationToken.ANONYMOUS, NavigatorProviderImpl.getConnectionInfo(request),
                    method + "[JSONFILE]", getExternalRequest(new ExternalRequest.Param[]{fileParam}, request))));
        } catch (IOException | AppServerNotAvailableDispatchException e) {
            throw Throwables.propagate(e);
        }
    }

    public static LogicsSessionObject.InitSettings getInitSettings(RemoteNavigatorInterface remoteNavigator, HttpServletRequest request, ClientInfo clientInfo) throws RemoteException {
        String screenSize = null;
        String scale = null;

        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("LSFUSION_SCREEN_SIZE")) {
                    screenSize = cookie.getValue();
                } else if(cookie.getName().equals("LSFUSION_SCALE")) {
                    scale = cookie.getValue();
                }
            }
        }
        if (screenSize != null) {
            clientInfo.screenSize = screenSize;
        }
        if (scale != null) {
            clientInfo.scale = Double.parseDouble(scale);
        }

        remoteNavigator.updateClientInfo(clientInfo);
        return LogicsSessionObject.getInitSettings(NavigatorProviderImpl.getSessionInfo(request), remoteNavigator);
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

    private String getPWAIcon(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.PWAIcon != null ? getFileUrl(serverSettings.PWAIcon) : "static/noauth/images/pwa-icon.png";
    }

    private String getLogicsIcon(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.logicsIcon != null ? getFileUrl(serverSettings.logicsIcon) : "static/noauth/images/favicon.ico";
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

    private String getFileUrl(FileData file) {
        assert file != null;
        return FileUtils.saveApplicationFile(file);
    }

    public static ExternalRequest getExternalRequest(ExternalRequest.Param[] params, HttpServletRequest request){
        String contentTypeString = request.getContentType();

        return new ExternalRequest(params, request.getScheme(), request.getMethod(), request.getServerName(), request.getServerPort(), request.getContextPath(),
                request.getServletPath(), request.getPathInfo() == null ? "" : request.getPathInfo(), request.getQueryString() != null ? request.getQueryString() : "",
                contentTypeString, request.getSession().getId());
    }

    public static String getURLPreservingParameters(String url, List<String> paramsToRemove, HttpServletRequest request) {
        return getURLPreservingParameters(url, null, paramsToRemove, request);
    }
    public static String getURLPreservingParameters(String url, String query, List<String> paramsToRemove, HttpServletRequest request) {
        String requestQuery = getQueryPreservingParameters(paramsToRemove, request);
        if(query != null) {
            assert !query.isEmpty();
            requestQuery = requestQuery.isEmpty() ? query : requestQuery + "&" + query;
        }
        return url + (!requestQuery.isEmpty() ? "?" + requestQuery : "");
    }

    public static String getQueryPreservingParameters(List<String> paramsToRemove, HttpServletRequest request) {
        String queryString = request.getQueryString();
        if(queryString == null)
            queryString = "";

        if (paramsToRemove != null && !queryString.isEmpty())
            queryString = Arrays.stream(queryString.split("&")).filter(s -> (paramsToRemove.stream().noneMatch(s::contains))).collect(Collectors.joining("&"));

        return queryString;
    }

    public static String getDirectUrl(String url, List<String> paramsToRemove, String query, HttpServletRequest request) {
        return request.getContextPath() + getURLPreservingParameters(url, query, paramsToRemove, request);
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