package com.example.application.handlers;


import com.example.application.exceptions.ForbiddenException;
import com.example.application.exceptions.UnauthorizedException;
import com.example.application.views.auth.LoginView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GlobalErrorHandler implements ErrorHandler {


    @Override
    public void error(ErrorEvent event) {
        Throwable throwable = event.getThrowable();

        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        if (throwable instanceof UnauthorizedException) {
            handleUnauthorized();
        } else if (throwable instanceof ForbiddenException) {
            handleForbidden();
        } else {
            handleGenericError(throwable);
        }
    }

    private void handleUnauthorized() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> {
                VaadinSession.getCurrent().close();
                ui.navigate(LoginView.class);
                Notification.show("Сессия истекла. Требуется повторная авторизация", 5000, Notification.Position.TOP_CENTER);
            });
        }
    }

    private void handleForbidden() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> {
                Notification.show("Доступ запрещен.", 5000, Notification.Position.TOP_CENTER);
                ui.navigate(LoginView.class);
            });
        }
    }

    private void handleGenericError(Throwable throwable) {
        log.error("Произошла ошибка: ", throwable);
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> {
                Notification.show("Ошибка: " + throwable.getMessage(), 5000, Notification.Position.TOP_CENTER);
            });
        }
    }
}
