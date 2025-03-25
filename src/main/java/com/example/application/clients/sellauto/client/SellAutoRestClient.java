package com.example.application.clients.sellauto.client;


import com.example.application.clients.sellauto.payloads.*;
import com.example.application.exceptions.ForbiddenException;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.exceptions.UnauthorizedException;
import com.example.application.handlers.GlobalErrorHandler;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.function.Supplier;


@Component
@Slf4j
public class SellAutoRestClient {

    private final RestClient restClient;


    private final static String LOGIN_URL = "/api/v1/auth/login";
    private final static String SING_UP_URL = "/api/v1/auth/sing-up";
    private final static String REFRESH_URL = "/api/v1/auth/refresh";
    private final static String PROFILE_URL = "/api/v1/profile";
    private final static String ADS_LIST_URL = "/api/v1/ads";

    public SellAutoRestClient(@Value("${sell-auto.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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
                            .uri(PROFILE_URL)
                            .retrieve()
                            .body(ProfilePayload.class)
            );
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
            throw new UnauthorizedException("Вы не авторизованы.");
        }

        if (statusCode == HttpStatus.FORBIDDEN) {
            log.warn("Received 403 Forbidden response");
            throw new ForbiddenException("Ошибка доступа");
        }

        if (statusCode.is5xxServerError()) {
            throw new SellAutoApiException("Server error: " + statusCode);
        }

        if (!statusCode.is2xxSuccessful()) {
            throw new SellAutoApiException(new String(response.getBody().readAllBytes()));
        }
        return false;
    }
}
