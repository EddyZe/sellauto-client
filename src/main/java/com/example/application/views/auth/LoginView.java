package com.example.application.views.auth;

import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.profile.ProfileView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;


@PageTitle("Вход")
@Route(value = "login", layout = MainLayout.class)
public class LoginView extends VerticalLayout {
    private final SellAutoRestClient client;

    public LoginView(SellAutoRestClient client) {
        this.client = client;
        setSizeFull();
        if (client.getCurrentLogin() != null) {
            UI.getCurrent().navigate(ProfileView.class);
            return;
        }
        initView();
    }

    private void initView() {
        TextField email = new TextField("Email");
        PasswordField password = new PasswordField("Пароль");
        Button loginButton = createLoginButton(email, password);

        FormLayout formLayout = createLoginFormLayout(email, password, loginButton);

        setAlignItems(Alignment.CENTER);
        setHorizontalComponentAlignment(Alignment.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        add(formLayout);
    }

    private Button createLoginButton(TextField email, PasswordField password) {
        return new Button("Вход", e -> {
            try {
                UI.getCurrent().access(() -> {
                    try {
                        client.auth(email.getValue(), password.getValue());
                        UI.getCurrent().getPage().open("profile");
                    } catch (SellAutoApiException ex) {
                        Notification.show("Не верный логин или пароль!", 5000, Notification.Position.TOP_CENTER);
                    }
                });
            } catch (Exception ex) {
                Notification.show("Unexpected error: " + ex.getMessage());
            }
        });
    }

    private FormLayout createLoginFormLayout(TextField email, PasswordField password, Button loginButton) {
        H1 title = new H1("Вход");
        title.setWidthFull();
        RouterLink routerLink = new RouterLink("Регистрация", SingUpView.class);
        Span span = new Span("Нет аккаунта?");
        var hor = new HorizontalLayout(span, routerLink);

        FormLayout formLayout = new FormLayout();
        formLayout.add(title, email, password, loginButton, hor);
        formLayout.setColspan(title, 2);
        formLayout.setColspan(email, 2);
        formLayout.setColspan(password, 2);
        formLayout.setColspan(loginButton, 2);
        formLayout.setColspan(hor, 1);
        formLayout.setWidth("30%");
        formLayout.setClassName("custom-card");
        return formLayout;
    }
}
