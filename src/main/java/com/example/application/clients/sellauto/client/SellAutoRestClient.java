package com.example.application.clients.sellauto.client;


import com.example.application.clients.sellauto.payloads.*;
import com.example.application.exceptions.ForbiddenException;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.exceptions.UnauthorizedException;
import com.example.application.handlers.GlobalErrorHandler;
import com.example.application.views.auth.LoginView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


@Component
@Slf4j
public class SellAutoRestClient {

    private final RestClient restClient;


    private final static String LOGIN_URL = "/api/v1/auth/login";
    private final static String SING_UP_URL = "/api/v1/auth/sing-up";
    private final static String REFRESH_URL = "/api/v1/auth/refresh";
    private final static String PROFILE_URL = "/api/v1/profiles";
    private final static String ADS_LIST_URL = "/api/v1/ads";
    private final static String BRANDS_URL = "/api/v1/brands";
    private final static String COLORS_URL = "/api/v1/colors";
    private final static String CHATS_URL = "/api/v1/chats";
    private final static String FEEDBACK_URL = "/api/v1/feedbacks";
    private final static String LOGOUT = "/logout";
    private final static String ADMIN_URL = "/api/v1/admin";
    private final static String SEND_RECOVERY_CODE = "/api/v1/auth/sendRecoveryCode";
    private final static String RESET_PASSWORD = "/api/v1/auth/resetPassword";


    public SellAutoRestClient(@Value("${sell-auto.base-url}") String baseUrl, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .messageConverters(converts -> {
                    converts.add(new MappingJackson2HttpMessageConverter(objectMapper));
                    converts.add(new FormHttpMessageConverter());
                    converts.add(new ResourceHttpMessageConverter());
                })
                .defaultStatusHandler(this::handleErrorResponse)
                .requestInterceptor((request, body, execution) -> {
                    addAuthHeader(request);
                    return execution.execute(request, body);
                })
                .build();
    }

    public void auth(String email, String password) {
        var response = login(email, password);
        setSessionAttribute(LoginResponse.class, response);
    }

    public void singUp(SingUpPayload sing) {
        try {
            restClient.post()
                    .uri(SING_UP_URL)
                    .body(sing)
                    .retrieve()
                    .body(ProfilePayload.class);
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void sendRecoveryCode(String email) {
        try {
            restClient.post()
                    .uri(SEND_RECOVERY_CODE)
                    .body(RequestSendRecoveryCodePayload.builder()
                            .email(email)
                            .build())
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new SellAutoApiException("Что-то пошло не так");
        }
    }

    public void resetPassword(ResetPasswordPayload resetPassword) {
        try {
            restClient.post()
                    .uri(RESET_PASSWORD)
                    .body(resetPassword)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new SellAutoApiException("Проверьте валидность введенного пароля и кода.");
        }
    }

    public FeedBackPayload sendFeedBack(NewFeedBackPayload newFeedback) {
        return executeWithTokenRefresh(() -> restClient
                .post()
                .uri(FEEDBACK_URL)
                .body(newFeedback)
                .retrieve()
                .body(FeedBackPayload.class));
    }

    public void deleteFeedBack(Long id) {
        executeWithTokenRefresh(() ->
                restClient
                        .delete()
                        .uri(FEEDBACK_URL + "/" + id)
                        .retrieve()
                        .toBodilessEntity());
    }

    public void logout() {
        try {
            executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(LOGOUT)
                            .retrieve()
                            .toBodilessEntity());
        } catch (SellAutoApiException e) {
            log.error(e.getMessage());
        } finally {
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                session.close();
            }
            SecurityContextHolder.clearContext();

            UI.getCurrent().accessSynchronously(() ->
                    UI.getCurrent().navigate(LoginView.class));
        }
    }


    public UserFeedBackPayload getUserFeedBack(Long userId) {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(FEEDBACK_URL + "/" + userId)
                        .retrieve()
                        .body(UserFeedBackPayload.class));
    }

    public ChatsDetailsPayload getMyChats() {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(CHATS_URL + "/my")
                        .retrieve()
                        .body(ChatsDetailsPayload.class));
    }

    public ChatDetailsPayload getChat(Long chatId) {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(CHATS_URL + "/" + chatId)
                        .retrieve()
                        .body(ChatDetailsPayload.class));
    }

    public ChatMessagesPayload getChatMessages(Long chatId) {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(CHATS_URL + "/%d/messages".formatted(chatId))
                        .retrieve()
                        .body(ChatMessagesPayload.class));
    }

    public AdsDetailsPayload getAds() {
        try {
            return restClient.get()
                    .uri(ADS_LIST_URL)
                    .retrieve()
                    .body(AdsDetailsPayload.class);
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void editAd(EditAdPayload editAd, Long id) {
        executeWithTokenRefresh(() ->
                restClient.patch()
                        .uri(ADS_LIST_URL + "/" + id)
                        .body(editAd)
                        .retrieve()
                        .toBodilessEntity());
    }

    public AdsDetailsPayload getAds(Map<String, String> params) {
        var sb = new StringBuilder();
        params.forEach((k, v) -> sb.append("&")
                .append(k)
                .append("=")
                .append(v));
        try {
            if (sb.length() <= 1) {
                return getAds();
            }

            return restClient.get()
                    .uri(ADS_LIST_URL + "?" + sb.substring(1))
                    .retrieve()
                    .body(AdsDetailsPayload.class);
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public Resource getPhoto(Long photoId) {
        try {
            return restClient.get()
                    .uri(ADS_LIST_URL + "/getPhoto/" + photoId)
                    .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM))
                    .retrieve()
                    .body(Resource.class);
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void deleteAd(Long id) {
        executeWithTokenRefresh(() ->
                restClient.delete()
                        .uri(ADS_LIST_URL + "/" + id)
                        .retrieve()
                        .toBodilessEntity());
    }

    public ChatBasePayload openChat(Long chatId) {
        return executeWithTokenRefresh(() ->
                restClient.post()
                        .uri(CHATS_URL + "/" + chatId)
                        .retrieve()
                        .body(ChatBasePayload.class));
    }

    public Resource downloadBackup() {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(ADMIN_URL + "/backup/download")
                        .retrieve()
                        .body(Resource.class));
    }

    public void uploadBackup(Resource resource) {
        var mbb = new MultipartBodyBuilder();
        mbb.part("file", resource, MediaType.APPLICATION_OCTET_STREAM);

        executeWithTokenRefresh(() ->
                restClient.post()
                        .uri(ADMIN_URL + "/backup/upload")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(mbb.build())
                        .retrieve()
                        .toBodilessEntity());
    }

    public ColorsPayload getAdminColors() {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(ADMIN_URL + "/colors")
                        .retrieve()
                        .body(ColorsPayload.class));
    }

    public ColorsPayload getColors() {
        try {
            return restClient.get()
                    .uri(COLORS_URL)
                    .retrieve()
                    .body(ColorsPayload.class);
        } catch (Exception e) {
            log.error("error", e);
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public ColorBasePayload getColorTitle(String tile) {
        try {
            return restClient.get()
                    .uri(COLORS_URL + "/" + tile)
                    .retrieve()
                    .body(ColorBasePayload.class);
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void createColor(ColorBasePayload color) {
        executeWithTokenRefresh(() ->
                restClient.post()
                        .uri(ADMIN_URL + "/colors")
                        .body(color)
                        .retrieve()
                        .toBodilessEntity());
    }

    public void deleteColor(Integer colorId) {
        executeWithTokenRefresh(() ->
                restClient.delete()
                        .uri(ADMIN_URL + "/colors/" + colorId)
                        .retrieve()
                        .toBodilessEntity());
    }

    public BrandsPayload getAdminBrands() {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(ADMIN_URL + "/brands")
                        .retrieve()
                        .body(BrandsPayload.class)
        );
    }

    public BrandsPayload getBrands() {
        try {
            return restClient.get()
                    .uri(BRANDS_URL)
                    .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_JSON))
                    .retrieve()
                    .body(BrandsPayload.class);
        } catch (Exception e) {
            log.error("error", e);
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void deleteModel(Integer modelId) {
        executeWithTokenRefresh(() ->
                restClient.delete()
                        .uri(ADMIN_URL + "/models/" + modelId)
                        .retrieve()
                        .toBodilessEntity());
    }

    public void createBrand(BrandBasePayload brandBasePayload) {
        executeWithTokenRefresh(() ->
                restClient.post()
                        .uri(ADMIN_URL + "/brands")
                        .body(brandBasePayload)
                        .retrieve()
                        .toBodilessEntity());
    }

    public BrandDetailPayload getBrand(Integer brandId) {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(ADMIN_URL + "/brands/" + brandId)
                        .retrieve()
                        .body(BrandDetailPayload.class));
    }

    public void deleteBrand(Integer brandId) {
        executeWithTokenRefresh(() ->
                restClient.delete()
                        .uri(ADMIN_URL + "/brands/" + brandId)
                        .retrieve()
                        .toBodilessEntity());
    }

    public ModelsPayload getModelsFromBrandTitle(String brandTitle) {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(ADMIN_URL + "/brand/" + brandTitle + "/models")
                        .retrieve()
                        .body(ModelsPayload.class));
    }

    public void createModel(ModelBasePayload model, String brandTitle) {
        executeWithTokenRefresh(() ->
                restClient.post()
                        .uri(ADMIN_URL + "/models/" + brandTitle)
                        .body(model)
                        .retrieve()
                        .toBodilessEntity());
    }

    public AdsDetailsPayload getUserAdsDetails(Long userId) {
        try {
            return restClient.get()
                    .uri(ADS_LIST_URL + "/user/" + userId)
                    .retrieve()
                    .body(AdsDetailsPayload.class);
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }


    public AdPayload createAd(CreateNewAdPayload createNewAdPayload, List<Resource> photos) {
        var mbb = new MultipartBodyBuilder();
        photos.forEach(photo -> mbb.part("files", photo));
        mbb.part("ad", createNewAdPayload);
        System.out.println("Request body: " + mbb.build());
        return executeWithTokenRefresh(() -> restClient.post()
                .uri(ADS_LIST_URL + "/create")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(mbb.build())
                .retrieve()
                .body(AdPayload.class)
        );
    }

    public AdPayload getAdId(Long id) {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(ADS_LIST_URL + "/" + id)
                        .retrieve()
                        .body(AdPayload.class));
    }

    public void editProfile(EditProfilePayload editProfile) {
        executeWithTokenRefresh(() ->
                restClient.patch()
                        .uri(PROFILE_URL)
                        .body(editProfile)
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    private LoginResponse login(String email, String password) {
        try {
            return restClient.post()
                    .uri(LOGIN_URL)
                    .body(LoginRequest.builder()
                            .email(email)
                            .password(password)
                            .build())
                    .retrieve()
                    .body(LoginResponse.class);
        } catch (Exception e) {
            throw new SellAutoApiException("Не верный логин или пароль");
        }
    }

    private void refreshToken() {
        LoginResponse currentLogin = getCurrentLogin();
        if (currentLogin == null) {
            VaadinSession.getCurrent().setAttribute(LoginResponse.class, null);
            throw new SellAutoApiException("Not authenticated");
        }

        try {
            var refreshToken = currentLogin.getRefreshToken();
            LoginResponse newResponse = restClient.post()
                    .uri(REFRESH_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
                    .retrieve()
                    .body(LoginResponse.class);

            VaadinSession.getCurrent().setAttribute(LoginResponse.class, newResponse);
        } catch (Exception ex) {
            VaadinSession.getCurrent().setAttribute(LoginResponse.class, null);
            throw new SellAutoApiException("Token refresh failed");
        }
    }

    private <T> T executeWithTokenRefresh(Supplier<T> requestSupplier) {
        try {
            return requestSupplier.get();
        } catch (UnauthorizedException ex) {
            try {
                refreshToken();
                return requestSupplier.get();
            } catch (ForbiddenException e) {
                throw new ForbiddenException(e.getMessage());
            } catch (Exception refreshEx) {
                VaadinSession.getCurrent().close();
                UI.getCurrent().navigate(LoginView.class);
                return null;
            }
        }
    }

    public ProfilePayload getProfile() {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(PROFILE_URL + "/my")
                        .retrieve()
                        .body(ProfilePayload.class)
        );
    }

    public ProfilePayload getProfileById(Long id) {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(PROFILE_URL + "/" + id)
                        .retrieve()
                        .body(ProfilePayload.class));
    }

    public ProfilesPayload getProfiles() {
        return executeWithTokenRefresh(() ->
                restClient.get()
                        .uri(PROFILE_URL)
                        .retrieve()
                        .body(ProfilesPayload.class));
    }

    public void banAccount(Long accountId) {
        executeWithTokenRefresh(() ->
                restClient.post()
                        .uri(ADMIN_URL + "/ban/" + accountId)
                        .retrieve()
                        .toBodilessEntity());
    }

    public void unBanAccount(Long accountId) {
        executeWithTokenRefresh(() ->
                restClient.post()
                        .uri(ADMIN_URL + "/unban/" + accountId)
                        .retrieve()
                        .toBodilessEntity());
    }


    private void addAuthHeader(HttpRequest request) {
        LoginResponse loginResponse = getCurrentLogin();

        if (request.getURI().getPath().endsWith(REFRESH_URL)) {
            return;
        }

        if (loginResponse != null) {
            request.getHeaders().setBearerAuth(loginResponse.getAccessToken());
        }
    }

    public LoginResponse getCurrentLogin() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            return ui.getSession().getAttribute(LoginResponse.class);
        }
        return null;
    }

    private <T> void setSessionAttribute(Class<T> type, T value) {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.accessSynchronously(() -> {
                VaadinSession session = VaadinSession.getCurrent();
                if (session != null) {
                    session.setErrorHandler(new GlobalErrorHandler());
                    session.setAttribute(type, value);
                    log.debug("Session attribute set: {}", type.getSimpleName());
                }
            });
        }
    }

    private boolean handleErrorResponse(ClientHttpResponse response) throws IOException {
        var statusCode = response.getStatusCode();

        if (statusCode == HttpStatus.UNAUTHORIZED) {
            log.warn("Received 401 Unauthorized response");
            throw new UnauthorizedException("Unauthorized");
        }

        if (statusCode == HttpStatus.FORBIDDEN) {
            log.warn("Received 403 Forbidden response");
            throw new ForbiddenException("Forbidden");
        }

        if (statusCode.is5xxServerError()) {
            log.warn("Received 5xx Server Error");
            throw new SellAutoApiException("Server error: " + statusCode);
        }

        if (!statusCode.is2xxSuccessful()) {
            throw new SellAutoApiException(new String(response.getBody().readAllBytes()));
        }
        return false;
    }
}
