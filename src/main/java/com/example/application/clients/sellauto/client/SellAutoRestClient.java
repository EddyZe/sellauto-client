package com.example.application.clients.sellauto.client;


import com.example.application.clients.sellauto.payloads.*;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.exceptions.UnauthorizedException;
import com.example.application.handlers.GlobalErrorHandler;
import com.example.application.views.auth.LoginView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;


@Component
@Slf4j
public class SellAutoRestClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;


    private final static String LOGIN_URL = "/api/v1/auth/login";
    private final static String SING_UP_URL = "/api/v1/auth/sing-up";
    private final static String REFRESH_URL = "/api/v1/auth/refresh";
    private final static String PROFILE_URL = "/api/v1/profiles";
    private final static String ADS_LIST_URL = "/api/v1/ads";
    private final static String BRANDS_URL = "/api/v1/brands";
    private final static String COLORS_URL = "/api/v1/colors";

    private final static String ADMIN_URL = "/api/v1/admin";


    public SellAutoRestClient(@Value("${sell-auto.base-url}") String baseUrl, ObjectMapper objectMapper, ObjectMapper objectMapper1) {
        this.objectMapper = objectMapper1;
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

//    private AdsPayload getAds() {
//        try {
//
//        } catch (Exception e) {
//            throw new SellAutoApiException("Ошибка получения списка объявлений!");
//        }
//    }

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

    public Resource downloadBackup() {
        try {
            return executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(ADMIN_URL + "/backup/download")
                            .retrieve()
                            .body(Resource.class));
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void uploadBackup(Resource resource) {
        try {
            var mbb = new MultipartBodyBuilder();
            mbb.part("file", resource, MediaType.APPLICATION_OCTET_STREAM);

            executeWithTokenRefresh(() ->
                    restClient.post()
                            .uri(ADMIN_URL + "/backup/upload")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(mbb.build())
                            .retrieve()
                            .toBodilessEntity());
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public ColorsPayload getAdminColors() {
        try {
            return executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(ADMIN_URL + "/colors")
                            .retrieve()
                            .body(ColorsPayload.class));
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
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
        try {
            executeWithTokenRefresh(() ->
                    restClient.post()
                            .uri(ADMIN_URL + "/colors")
                            .body(color)
                            .retrieve()
                            .toBodilessEntity());
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void deleteColor(Integer colorId) {
        try {
            executeWithTokenRefresh(() ->
                    restClient.delete()
                            .uri(ADMIN_URL + "/colors/" + colorId)
                            .retrieve()
                            .toBodilessEntity());
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public BrandsPayload getAdminBrands() {
        try {
            return executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(ADMIN_URL + "/brands")
                            .retrieve()
                            .body(BrandsPayload.class)
            );
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
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
        try {
            executeWithTokenRefresh(() ->
                    restClient.delete()
                            .uri(ADMIN_URL + "/models/" + modelId)
                            .retrieve()
                            .toBodilessEntity());

        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void createBrand(BrandBasePayload brandBasePayload) {
        try {
            executeWithTokenRefresh(() ->
                    restClient.post()
                            .uri(ADMIN_URL + "/brands")
                            .body(brandBasePayload)
                            .retrieve()
                            .toBodilessEntity());
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public BrandDetailPayload getBrand(Integer brandId) {
        try {
            return executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(ADMIN_URL + "/brands/" + brandId)
                            .retrieve()
                            .body(BrandDetailPayload.class));
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void deleteBrand(Integer brandId) {
        try {
            executeWithTokenRefresh(() ->
                    restClient.delete()
                            .uri(ADMIN_URL + "/brands/" + brandId)
                            .retrieve()
                            .toBodilessEntity());
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public ModelsPayload getModelsFromBrandTitle(String brandTitle) {
        try {
            return executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(ADMIN_URL + "/brand/" + brandTitle + "/models")
                            .retrieve()
                            .body(ModelsPayload.class));
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void createModel(ModelBasePayload model, String brandTitle) {
        try {
            executeWithTokenRefresh(() ->
                    restClient.post()
                            .uri(ADMIN_URL + "/models/" + brandTitle)
                            .body(model)
                            .retrieve()
                            .toBodilessEntity());
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public UserAdsDetailsPayload getUserAdsDetails(Long userId) {
        try {
            return restClient.get()
                    .uri(ADS_LIST_URL + "/user/" + userId)
                    .retrieve()
                    .body(UserAdsDetailsPayload.class);
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }


    public AdPayload createAd(CreateNewAdPayload createNewAdPayload, List<Resource> photos) {
        try {
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
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public AdPayload getAdId(Long id) {
        try {
            return executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(ADS_LIST_URL + "/" + id)
                            .retrieve()
                            .body(AdPayload.class));
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void editProfile(EditProfilePayload editProfile) {
        try {
            executeWithTokenRefresh(() ->
                    restClient.patch()
                            .uri(PROFILE_URL)
                            .body(editProfile)
                            .retrieve()
                            .toBodilessEntity()
            );
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
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
            throw new SellAutoApiException("Not authenticated");
        }

        try {
            LoginResponse newResponse = restClient.post()
                    .uri(REFRESH_URL)
                    .headers(headers -> headers.setBearerAuth(currentLogin.getRefreshToken()))
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
            } catch (Exception refreshEx) {
                VaadinSession.getCurrent().close();
                throw new UnauthorizedException("Session expired. Please relogin");
            }
        }
    }

    public ProfilePayload getProfile() {
        try {
            return executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(PROFILE_URL + "/my")
                            .retrieve()
                            .body(ProfilePayload.class)
            );
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public ProfilePayload getProfileById(Long id) {
        try {
            return executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(PROFILE_URL + "/" + id)
                            .retrieve()
                            .body(ProfilePayload.class));
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public ProfilesPayload getProfiles() {
        try {
            return executeWithTokenRefresh(() ->
                    restClient.get()
                            .uri(PROFILE_URL)
                            .retrieve()
                            .body(ProfilesPayload.class));
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void banAccount(Long accountId) {
        try {
            executeWithTokenRefresh(() ->
                    restClient.post()
                            .uri(ADMIN_URL + "/ban/" + accountId)
                            .retrieve()
                            .toBodilessEntity());
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }

    public void unBanAccount(Long accountId) {
        try {
            executeWithTokenRefresh(() ->
                    restClient.post()
                            .uri(ADMIN_URL + "/unban/" + accountId)
                            .retrieve()
                            .toBodilessEntity());
        } catch (Exception e) {
            throw new SellAutoApiException(e.getMessage());
        }
    }


    private void addAuthHeader(HttpRequest request) {
        LoginResponse loginResponse = getCurrentLogin();
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
            UI.getCurrent().navigate(LoginView.class);
            throw new UnauthorizedException("Unauthorized");
        }

        if (statusCode == HttpStatus.FORBIDDEN) {
            log.warn("Received 403 Forbidden response");
            Notification.show("Ошибка доступа.", 5000, Notification.Position.TOP_CENTER);
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
