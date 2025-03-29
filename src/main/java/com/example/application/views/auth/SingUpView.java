package com.example.application.views.auth;

import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.SingUpPayload;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.profile.ProfileView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;


@PageTitle("Регистрация")
@Route(value = "sing-up")
public class SingUpView extends VerticalLayout {

    private final SellAutoRestClient client;

    private Binder<SingUpPayload> binder = new BeanValidationBinder<>(SingUpPayload.class);

    public SingUpView(SellAutoRestClient client) {
        this.client = client;
        setSizeFull();
        if (client.getCurrentLogin() != null) {
            UI.getCurrent().navigate(ProfileView.class);
            return;
        }
        initView();
    }

    private void initView() {

        TextField firstName = new TextField("Имя");
        firstName.setRequired(true);
        TextField lastName = new TextField("Фамилия");
        lastName.setRequired(true);
        EmailField email = new EmailField("Email");
        email.setRequired(true);
        TextField phoneNumber = new TextField("Номер телефона");
        phoneNumber.setRequired(true);
        PasswordField password = new PasswordField("Пароль");
        password.setRequired(true);
        configureBinder(email, phoneNumber, password, firstName, lastName);

        Button create = createSingUpButton(firstName, lastName, phoneNumber, email, password);

        FormLayout formLayout = createLoginFormLayout(email, password, create, firstName, lastName, phoneNumber);

        setAlignItems(Alignment.CENTER);
        setHorizontalComponentAlignment(Alignment.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        add(formLayout);
    }

    private Button createSingUpButton(TextField firstName, TextField lastName, TextField phoneNumber, EmailField email, PasswordField password) {
        return new Button("Регистрация", e -> {
            try {
                UI.getCurrent().access(() -> {
                    try {
                        if (firstName.getValue().isEmpty() || lastName.getValue().isEmpty() || email.getValue().isEmpty() || phoneNumber.getValue().isEmpty()) {
                            Notification.show("Поля должны быть все заполнены!", 5000, Notification.Position.TOP_CENTER);
                            return;
                        }

                        var sing = SingUpPayload.builder()
                                .lastName(lastName.getValue())
                                .firstName(firstName.getValue())
                                .phoneNumber(phoneNumber.getValue())
                                .email(email.getValue())
                                .password(password.getValue())
                                .build();
                        client.singUp(sing);

                        UI.getCurrent().getPage().open("login");
                    } catch (SellAutoApiException ex) {
                        if (ex.getMessage().toLowerCase().contains("email is already")) {
                            Notification.show("Email уже занят!", 5000, Notification.Position.TOP_CENTER);
                            return;
                        }

                        if (ex.getMessage().toLowerCase().contains("phone number is already")) {
                            Notification.show("Номер телефона уже занят!", 5000, Notification.Position.TOP_CENTER);
                            return;
                        }

                        Notification.show("Исправьте ошибки в полях!", 5000, Notification.Position.TOP_CENTER);
                    }
                });
            } catch (Exception ex) {
                Notification.show("Unexpected error: " + ex.getMessage());
            }
        });
    }

    private FormLayout createLoginFormLayout(EmailField email, PasswordField password, Button loginButton, TextField firstName, TextField lastName, TextField phoneNumber) {
        H1 title = new H1("Регистрация");
        title.setWidthFull();
        RouterLink routerLink = new RouterLink("Вход", LoginView.class);
        Span span = new Span("Есть аккаунт?");
        var hor = new HorizontalLayout(span, routerLink);

        FormLayout formLayout = new FormLayout();
        formLayout.add(title, firstName, lastName, phoneNumber, email, password, loginButton, hor);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        formLayout.setColspan(title, 2);
        formLayout.setColspan(firstName, 1);
        formLayout.setColspan(lastName, 1);
        formLayout.setColspan(phoneNumber, 2);
        formLayout.setColspan(email, 2);
        formLayout.setColspan(password, 2);
        formLayout.setColspan(loginButton, 2);
        formLayout.setColspan(hor, 1);
        formLayout.setWidth("30%");
        formLayout.setClassName("custom-card");
        return formLayout;
    }

    private void configureBinder(EmailField email, TextField phone, PasswordField password,
                                 TextField firstName, TextField lastName) {

        binder.forField(email)
                .withValidator(new EmailValidator("Некорректный email"))
                .bind(SingUpPayload::getEmail, SingUpPayload::setEmail);

        binder.forField(phone)
                .withValidator(new RegexpValidator(
                        "Некорректный формат телефона",
                        "^\\+?[0-9]{10,15}$"
                ))
                .bind(SingUpPayload::getPhoneNumber, SingUpPayload::setPhoneNumber);

        binder.forField(password)
                .withValidator(
                        new StringLengthValidator("Пароль должен быть 6-30 символов", 6, 20)
                )
                .bind(SingUpPayload::getPassword, SingUpPayload::setPassword);

        binder.forField(firstName)
                .withValidator(
                        new StringLengthValidator("Имя должно быть 2-30 символов", 2, 30)
                )
                .bind(SingUpPayload::getFirstName, SingUpPayload::setFirstName);

        binder.forField(lastName)
                .withValidator(
                        new StringLengthValidator("Фамилия должна быть 2-30 символов", 2, 30)
                )
                .bind(SingUpPayload::getLastName, SingUpPayload::setLastName);
    }

}
