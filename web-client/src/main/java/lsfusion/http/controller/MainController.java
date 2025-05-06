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
import lsfusion.gwt.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.authentication.LSFClientRegistrationRepository;
import lsfusion.http.authentication.LSFRemoteAuthenticationProvider;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ClientType;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.navigator.ClientInfo;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.SessionInfo;
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
import javax.servlet.http.HttpServletResponse;
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

    public static ClientSettings getClientSettings(RemoteNavigatorInterface remoteNavigator, ServerSettings serverSettings, HttpServletRequest request, ClientInfo clientInfo) throws RemoteException {
        remoteNavigator.updateClientInfo(clientInfo);
        return LogicsSessionObject.getClientSettings(MainController.getExternalRequest(new ExternalRequest.Param[0], request), remoteNavigator, ClientFormChangesToGwtConverter.getConvertFileValue(request.getServletContext(), serverSettings));
    }

    public static String sendRequest(HttpServletRequest request, ExternalRequest.Param[] params, LogicsSessionObject sessionObject, String action) throws RemoteException {
        return sendRequest(AuthenticationToken.ANONYMOUS, request, params, sessionObject, NavigatorProviderImpl.getConnectionInfo(request), action);
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

    public static String sendRequest(AuthenticationToken token, HttpServletRequest request, ExternalRequest.Param[] params, LogicsSessionObject sessionObject, ConnectionInfo connectionInfo, String action) throws RemoteException {
        ExternalRequest externalRequest = MainController.getExternalRequest(params, request);
        ExternalResponse result = sessionObject.remoteLogics.exec(token, connectionInfo, action, externalRequest);
        return LogicsSessionObject.getStringResult(result, ClientFormChangesToGwtConverter.getConvertFileValue(sessionObject, request, connectionInfo, externalRequest));
    }

    public static LogicsSessionObject.InitSettings getInitSettings(RemoteNavigatorInterface remoteNavigator, ServerSettings serverSettings, HttpServletRequest request, ClientInfo clientInfo) throws RemoteException {
        String screenWidth = null;
        String screenHeight = null;
        String scale = null;

        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("LSFUSION_SCREEN_WIDTH")) {
                    screenWidth = cookie.getValue();
                } else if (cookie.getName().equals("LSFUSION_SCREEN_HEIGHT")) {
                    screenHeight = cookie.getValue();
                }else if(cookie.getName().equals("LSFUSION_SCALE")) {
                    scale = cookie.getValue();
                }
            }
        }
        if (screenWidth != null) {
            clientInfo.screenWidth = Integer.parseInt(screenWidth);
        }
        if (screenHeight != null) {
            clientInfo.screenHeight = Integer.parseInt(screenHeight);
        }
        if (scale != null) {
            clientInfo.scale = Double.parseDouble(scale);
        }

        remoteNavigator.updateClientInfo(clientInfo);
        SessionInfo sessionInfo = NavigatorProviderImpl.getSessionInfo(request);
        return LogicsSessionObject.getInitSettings(sessionInfo, remoteNavigator, ClientFormChangesToGwtConverter.getConvertFileValue(request.getServletContext(), serverSettings));
    }

    @RequestMapping(value = "/push-notification", method = RequestMethod.GET)
    public String pushNotification(ModelMap model, HttpServletRequest request) {
        model.addAttribute("id", request.getParameter(GwtSharedUtils.NOTIFICATION_PARAM));
        model.addAttribute("query", getQueryPreservingParameters(Collections.singletonList(GwtSharedUtils.NOTIFICATION_PARAM), request));
        addNoAuthStandardModelAttributes(model, request, getAndCheckServerSettings(request, checkVersionError, false));
        return "push-notification";
    }

    public static String getDirectUrl(String url, HttpServletRequest request) {
        return getDirectUrl(url, (String)null, request);
    }

    @RequestMapping(value = "/registration", method = RequestMethod.GET)
    public String registration(ModelMap model, HttpServletRequest request) {
        ServerSettings serverSettings = getAndCheckServerSettings(request, checkVersionError, false);
        addNoAuthStandardModelAttributes(model, request, serverSettings);
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
        if (jsonResponse.has("success")) {
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
        addNoAuthStandardModelAttributes(model, request, getAndCheckServerSettings(request, checkVersionError, false));
        return "forgot-password";
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

    @RequestMapping(value = "/forgot-password", method = RequestMethod.POST)
    public String processForgotPassword(@RequestParam String usernameOrEmail, HttpServletRequest request) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userNameOrEmail", usernameOrEmail);
        jsonArray.put(jsonObject);

        JSONObject jsonResponse = sendRequest(jsonArray, request, "Authentication.resetPassword");
        if (jsonResponse.has("success")) {
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
        addNoAuthStandardModelAttributes(model, request, getAndCheckServerSettings(request, checkVersionError, false));
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
        if (jsonResponse.has("success")) {
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", jsonResponse.optString("success"));
        } else if (jsonResponse.has("error")) {
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", jsonResponse.optString("error"));
            return getRedirectUrl("/change-password", jsonResponse.has("passwordStrengthError") ? null : Collections.singletonList("token"), request);
        }
        return getRedirectUrl("/login", Collections.singletonList("token"), request);
    }

    private void addNoAuthStandardModelAttributes(ModelMap model, HttpServletRequest request, ServerSettings serverSettings) {
        addStandardModelAttributes(model, request, serverSettings);

        addResourcesAttributes(model, serverSettings, true, serverSettings != null ? serverSettings.noAuthResourcesBeforeSystem : null, serverSettings != null ? serverSettings.noAuthResourcesAfterSystem : null);
    }

    public static String getDirectUrl(String url, String query, HttpServletRequest request) {
        return getDirectUrl(url, null, query, request);
    }

    private void addResourcesAttributes(ModelMap model, ServerSettings serverSettings, boolean noAuth, List<Pair<String, RawFileData>> resourcesBeforeSystem, List<Pair<String, RawFileData>> resourcesAfterSystem) {
        model.addAttribute("resourcesBeforeSystem", resourcesBeforeSystem != null ? saveResources(serverSettings, resourcesBeforeSystem, noAuth) : null);
        model.addAttribute("resourcesAfterSystem", resourcesAfterSystem != null ? saveResources(serverSettings, resourcesAfterSystem, noAuth) : null);
    }

    private ServerSettings getAndCheckServerSettings(HttpServletRequest request, Result<String> rCheck, boolean noCache) {
        ServerSettings serverSettings = getServerSettings(request, noCache);
        String checkVersionError = serverSettings != null ? BaseUtils.checkClientVersion(serverSettings.platformVersion, serverSettings.apiVersion, BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion()) : null;
        if (checkVersionError != null) {
            if (!noCache) // try without cache
                return getAndCheckServerSettings(request, rCheck, true);
            rCheck.set(checkVersionError);
        }
        return serverSettings;
    }

    private ServerSettings getServerSettings(HttpServletRequest request, boolean noCache) {
        return logicsProvider.getServerSettings(request, noCache);
    }

    public static String getDirectUrl(String url, List<String> paramsToRemove, HttpServletRequest request) {
        return getDirectUrl(url, paramsToRemove, null, request);
    }

    private List<WebAction> saveResources(ServerSettings serverSettings, List<Pair<String, RawFileData>> resources, boolean noAuth) {
        List<WebAction> versionedResources = new ArrayList<>();
        for (Pair<String, RawFileData> resource : resources) {
            String fullPath = resource.first;
            String extension;

            String url;
            boolean isUrl = false;
            if (resource.second != null) { // resource file
                extension = BaseUtils.getFileExtension(fullPath);
                url = extension.equals("html") ? resource.second.convertString() : FileUtils.saveWebFile(fullPath, resource.second, serverSettings, noAuth);
            } else { // url
                Result<String> rExtension = new Result<>();
                url = ClientActionToGwtConverter.convertUrl(fullPath, rExtension);
                extension = rExtension.result;
                isUrl = true;
            }
            versionedResources.add(new WebAction(url, extension, isUrl));
        }
        return versionedResources;
    }

    private JSONObject sendRequest(JSONArray jsonArray, HttpServletRequest request, String method) {
        try {
            return logicsProvider.runRequest(request,
                    (sessionObject, retry) -> new JSONObject(sendRequest(request, new ExternalRequest.Param[]{ExternalRequest.getSystemParam(jsonArray.toString())}, sessionObject, method + "[JSONFILE]")));
        } catch (IOException | AppServerNotAvailableDispatchException e) {
            throw Throwables.propagate(e);
        }
    }

    public static class WebAction {
        public final String resource;
        public final String extension;
        public final boolean isUrl;

        public WebAction(String resource, String extension, boolean isUrl) {
            this.resource = resource;
            this.extension = extension;
            this.isUrl = isUrl;
        }
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

    // to redirect on the page
    public static String getDirectUrl(String url, List<String> paramsToRemove, String query, HttpServletRequest request) {
        assert url.startsWith("/");
        return request.getContextPath() + getURLPreservingParameters(url, query, paramsToRemove, request);
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

/*
    needed contextPath (need getDirectURL):
        - In all links on the browser page
        - HttpServletResponse.sendRedirect

    contextPath is not needed:
        - getRedirectStrategy().sendRedirect
        - determineTargetUrl
        - "redirect:"
*/
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

    // to return in spring controller
    public static String getRedirectUrl(String url, List<String> paramsToRemove, HttpServletRequest request) {
        assert url.startsWith("/");
        return "redirect:" + getURLPreservingParameters(url, paramsToRemove, request);
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String processLogin(ModelMap model, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            if (auth instanceof LSFAuthenticationToken && ((LSFAuthenticationToken) auth).isAnonymous()) {
//                LSFLoginUrlAuthenticationEntryPoint.requestCache.saveRequest(request);
            } else {
                return getRedirectUrl("/main", null, request); // to prevent LSFAuthenticationSuccessHandler from showing login form twice (request cache)
            }
        }
        ServerSettings serverSettings = getAndCheckServerSettings(request, checkVersionError, false);

        model.addAttribute("disableRegistration", getDisableRegistration(serverSettings));
        model.addAttribute("registrationPage", getDirectUrl("/registration", request));
        model.addAttribute("forgotPasswordPage", getDirectUrl("/forgot-password", request));
        addNoAuthStandardModelAttributes(model, request, serverSettings);

        try {
            clientRegistrationRepository.iterator().forEachRemaining(registration -> oauth2AuthenticationUrls.put(registration.getRegistrationId(),
                    getDirectUrl(authorizationRequestBaseUri + registration.getRegistrationId(), request)));
            model.addAttribute("urls", oauth2AuthenticationUrls);
        } catch (Throwable e) {
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

    private void addStandardModelAttributes(ModelMap model, HttpServletRequest request, ServerSettings serverSettings) {
        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsLogo", getLogicsLogo(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));
        model.addAttribute("loginPage", getDirectUrl("/login", Collections.singletonList("token"), request));
        model.addAttribute("apiVersion", BaseUtils.getPlatformVersion() + " (" + BaseUtils.getApiVersion() + ")");
    }

    public static boolean isPrefetch(HttpServletRequest request) {
        return "prefetch".equals(request.getHeader("Sec-Purpose")) || "prefetch".equals(request.getHeader("Purpose"));
    }

    @RequestMapping(value = "/main", method = RequestMethod.GET)
    public String processMain(ModelMap model, HttpServletRequest request,
                                         HttpServletResponse response) {
//        this way it is really faster, when we start creating navigator when user still type (however later maybe it's better to parameterize this)
//        if (isPrefetch(request)) {
//            response.setStatus(HttpStatus.NO_CONTENT.value());
//            return null;
//        }

        ServerSettings serverSettings = getServerSettings(request, false);

        addStandardModelAttributes(model, request, serverSettings);

        model.addAttribute("logicsName", getLogicsName(serverSettings));
        model.addAttribute("lsfParams", getLsfParams(serverSettings));

        String sessionId;
        LogicsSessionObject.InitSettings initSettings;
        try {
            Result<LogicsSessionObject.InitSettings> rInitSettings = new Result<>();
            sessionId = logicsProvider.runRequest(request, (sessionObject, retry) -> {
                try {
                    String result = navigatorProvider.createNavigator(sessionObject, request);
                    rInitSettings.set(getInitSettings(navigatorProvider.getNavigatorSessionObject(result).remoteNavigator, serverSettings, request, new ClientInfo(1366, 768, 1.0, ClientType.WEB_DESKTOP, true)));
                    return result;
                } catch (RemoteMessageException e) {
                    request.getSession().setAttribute(AUTHENTICATION_EXCEPTION, new InternalAuthenticationServiceException(e.getMessage()));
                    throw e;
                }
            });
            initSettings = rInitSettings.result;
        } catch (AuthenticationException authenticationException) {
            return getRedirectUrl("/logout", null, request);
        } catch (Throwable e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("redirectURL", getDirectUrl("/main", request));
            return "app-not-available";
        }

        model.addAttribute("sessionID", sessionId);
        addResourcesAttributes(model, serverSettings, false, serverSettings != null ? initSettings.mainResourcesBeforeSystem : null, serverSettings != null ? initSettings.mainResourcesAfterSystem : null);

        return "main";
    }

    private String getJNLPUrls(HttpServletRequest request, ServerSettings serverSettings) {
        String directUrl = getDirectUrl("/exec", "action=Security.generateJnlp", request); //we use generateJnlp without params because linux mint cut from url '%5'
        String localizedString = ServerMessages.getString(request, "run.desktop.client");
        return serverSettings != null ? serverSettings.jnlpUrls
                .replaceAll("\\{runDesktopQuery}", directUrl)
                .replaceAll("\\{run.desktop.client}", localizedString)
                : "<a href=" + directUrl + ">" + localizedString + "</a>";
    }

    public static Authentication getAuthentication(HttpServletRequest request, String userName, String password, LSFRemoteAuthenticationProvider authenticationProvider) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userName, password);
        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetails(request));
        return authenticationProvider.authenticate(usernamePasswordAuthenticationToken);
    }
}