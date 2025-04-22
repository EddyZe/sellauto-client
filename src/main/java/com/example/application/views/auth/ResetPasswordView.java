package com.example.application.views.auth;

import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.ResetPasswordPayload;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;


@Route(value = "/resetPassword", layout = MainLayout.class)
@PageTitle("Сброс пароля")
public class ResetPasswordView extends VerticalLayout {

    private final SellAutoRestClient sellAutoRestClient;
    private final Binder<ResetPasswordPayload> binder = new BeanValidationBinder<>(ResetPasswordPayload.class);

    public ResetPasswordView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setAlignItems(Alignment.CENTER);
        setSizeFull();
        initFormResetPassword();
    }

    private void initFormResetPassword() {
        var resetBlock = new VerticalLayout();
        resetBlock.setClassName("custom-block");
        resetBlock.setWidth("50%");

        var title = new H1("Сброс пароля");
        var title2 = new Span("Код для восстановления пароля будет отправлен вам на электронную почту. Обратите внимание, что код действует, только 10 минут");
        var message = new Span("Код отправлен вам на email");
        message.setClassName("custom-notification-block");
        message.setWidthFull();
        message.setVisible(false);

        var emailAndSendCodeLay = new HorizontalLayout();
        emailAndSendCodeLay.setWidthFull();
        var emailField = new TextField();
        emailField.setPlaceholder("Введите email");
        emailField.setWidthFull();

        var sendCodeButton = new Button("Отправить код", e -> {
            try {
                if (emailField.getValue() == null || emailField.getValue().isEmpty()) {
                    Notification.show("Email не должен быть пустым!");
                    return;
                }
                sellAutoRestClient.sendRecoveryCode(emailField.getValue());
                message.setVisible(true);
            } catch (SellAutoApiException ex) {
                Notification.show(ex.getMessage(), 5000, Notification.Position.TOP_CENTER);
            }
        });
        emailAndSendCodeLay.add(emailField, sendCodeButton);

        var codeField = new NumberField("Полученный код");
        codeField.setWidthFull();
        codeField.setPlaceholder("Введите код");
        var newPasswordField = new PasswordField("Новый пароль");
        newPasswordField.setWidthFull();
        newPasswordField.setPlaceholder("Введите новый пароль");
        var resetPasswordButton = new Button("Сбросить пароль", e -> {
            try {
                if (codeField.getValue() == null) {
                    Notification.show("Введите код!", 5000, Notification.Position.TOP_CENTER);
                    return;
                }

                if (emailField.getValue() == null || emailField.getValue().isEmpty()) {
                    Notification.show("Email не должен быть пустым!");
                    return;
                }

                if (newPasswordField.getValue() == null || newPasswordField.getValue().isEmpty()) {
                    Notification.show("Введите новый пароль!", 5000, Notification.Position.TOP_CENTER);
                    return;
                }

                sellAutoRestClient.resetPassword(ResetPasswordPayload.builder()
                        .code(String.valueOf(codeField.getValue().intValue()))
                        .email(emailField.getValue())
                        .password(newPasswordField.getValue())
                        .build());
                UI.getCurrent().navigate(LoginView.class);
            } catch (SellAutoApiException ex) {
                Notification.show(ex.getMessage(), 5000, Notification.Position.TOP_CENTER);
            }
        });

        binder.forField(emailField)
                .withValidator(new EmailValidator("Некорректный email"))
                .bind(ResetPasswordPayload::getEmail, ResetPasswordPayload::setEmail);

        binder.forField(newPasswordField)
                .withValidator(
                        s -> {
                            if (s == null || s.length() < 6 || s.length() > 20) {
                                return false;
                            }

                            return s.matches("^(?=.*[A-Z])(?=.*[a-z]).+$");
                        }, "Пароль должен быть не менее 6 и не более 20 символов и содержать хотя бы одну строчную и заглавные буквы!"
                )
                .bind(ResetPasswordPayload::getPassword, ResetPasswordPayload::setPassword);
        resetBlock.add(title, title2, message, emailAndSendCodeLay, codeField, newPasswordField, resetPasswordButton);
        add(resetBlock);
    }

}
