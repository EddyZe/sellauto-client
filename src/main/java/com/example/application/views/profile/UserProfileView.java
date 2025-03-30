package com.example.application.views.profile;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.clients.sellauto.payloads.ProfilePayload;
import com.example.application.enums.Role;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.util.ComponentRenders;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;


@Route(value = "user", layout = MainLayout.class)
@PageTitle("Пользователь")
@Slf4j
public class UserProfileView extends HorizontalLayout implements HasUrlParameter<String> {

    private final SellAutoRestClient sellAutoRestClient;

    private final VerticalLayout infoLayout;
    private final VerticalLayout adsLayout;


    public UserProfileView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setSizeFull();

        infoLayout = new VerticalLayout();
        adsLayout = new VerticalLayout();
        infoLayout.setClassName("custom-block");
        adsLayout.setClassName("custom-block");
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, String s) {
        var userId = Long.parseLong(s);
        var user = sellAutoRestClient.getProfileById(userId);
        var currentUser = sellAutoRestClient.getProfile();

        var title = new H3(user.getFirstName());
        title.setWidthFull();
        var firstName = new Span("Имя: " + user.getFirstName());
        var lastName = new Span("Фамилия: " + user.getLastName());
        var numberPhone = new Span("Номер телефона: " + user.getAccount().getPhoneNumber());
        var email = new Span("Email: " + user.getAccount().getEmail());
        var countAds = new Span("Объявлений создано:  %d".formatted(user.getAds().size()));
        infoLayout.add(title, firstName, lastName, numberPhone, email, countAds);
        infoLayout.setClassName("custom-block");
        infoLayout.setWidth("40%");
        UI.getCurrent().getPage().setTitle(user.getFirstName());
        var titleAds = new H3("Объявления");
        titleAds.setWidthFull();
        adsLayout.add(titleAds);

        if (currentUser.getAccount().getRole() == Role.ROLE_ADMIN) {
            var blockedButton = createBlocedUserButton(user);
            infoLayout.add(blockedButton);
        }

        var userAds = new VirtualList<AdPayload>();
        userAds.setWidth("95%");
        userAds.setItems(sellAutoRestClient.getUserAdsDetails(user.getUserId()).getAds());
        if (!user.getAds().isEmpty()) {
            userAds.setRenderer(generateAdRender(currentUser, userAds, user));
            adsLayout.add(userAds);
        }

        add(infoLayout, adsLayout);
    }

    private ComponentRenderer<VerticalLayout, AdPayload> generateAdRender(ProfilePayload currentUser, VirtualList<AdPayload> userAds, ProfilePayload user) {
        return new ComponentRenderer<>(ad -> {
            var add = ComponentRenders.generateComponentAdList(ad,
                    sellAutoRestClient.getPhoto(
                            ad.getCar().getPhotos().getFirst().getPhotoId()
                    ),
                    "250px", "150px");

            if (currentUser.getAccount().getRole() == Role.ROLE_ADMIN) {
                var deleteAdButton = new Button("Удалить", e -> {
                    try {
                        sellAutoRestClient.deleteAd(ad.getAdId());
                        Notification.show("Объявление удалено", 5000, Notification.Position.TOP_CENTER);
                        userAds.setItems(sellAutoRestClient.getUserAdsDetails(user.getUserId()).getAds());
                    } catch (SellAutoApiException ex) {
                        log.error("error delete ad", ex);
                        Notification.show("Ошибка при удалении объявлении", 5000, Notification.Position.TOP_CENTER);
                    }
                });

                add.add(deleteAdButton);
            }
            return add;
        }
        );
    }

    private Button createBlocedUserButton(ProfilePayload user) {
        var blockedButton = new Button(user.getAccount().isBlocked() ? "Разблокировать" : "Заблокировать");
        blockedButton.addClickListener(event -> {
            try {
                if (user.getAccount().isBlocked()) {
                    sellAutoRestClient.unBanAccount(user.getAccount().getAccountId());
                    user.getAccount().setBlocked(false);
                    Notification.show("Пользователь разблокирован", 5000, Notification.Position.TOP_CENTER);
                    blockedButton.setText("Заблокировать");
                } else {
                    sellAutoRestClient.unBanAccount(user.getAccount().getAccountId());
                    user.getAccount().setBlocked(true);
                    Notification.show("Пользователь заблокирован", 5000, Notification.Position.TOP_CENTER);
                    blockedButton.setText("Разблокировать");
                }
            } catch (SellAutoApiException ex) {
                Notification.show("Что-то пошло не так. Попробуйте снова", 5000, Notification.Position.TOP_CENTER);
                log.error("error ban/unban user: {}", ex.getMessage(), ex);
            }
        });
        return blockedButton;
    }
}
