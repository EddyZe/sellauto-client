package com.example.application.views.profile;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.clients.sellauto.payloads.PhotoBasePayload;
import com.example.application.clients.sellauto.payloads.ProfilePayload;
import com.example.application.enums.Role;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.ads.Ad;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Objects;


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
        infoLayout.setSizeFull();

        adsLayout.setClassName("custom-block");
        adsLayout.setWidthFull();
        adsLayout.setHeight("100%");
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
        infoLayout.setWidthFull();
        UI.getCurrent().getPage().setTitle(user.getFirstName());
        var titleAds = new H3("Объявления пользователя");
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
            userAds.setRenderer(new ComponentRenderer<>(ad -> {
                var mainLay = new HorizontalLayout();
                var link = new RouterLink("%s %s рублей".formatted(ad.getTitle(), ad.getPrices().getLast().getPrice()), Ad.class,
                        ad.getAdId().toString());

                if (ad.getCar() != null && !ad.getCar().getPhotos().isEmpty()) {
                    var firstPhoto = ad.getCar().getPhotos().getFirst();
                    var resourse = sellAutoRestClient.getPhoto(firstPhoto.getPhotoId());
                    var photo = createImage(resourse, firstPhoto);
                    mainLay.setClassName("custom-card");
                    mainLay.add(photo);
                }

                var car = ad.getCar();
                var carName = new Span("%s %s %s. Пробег: %d км".formatted(Objects.requireNonNull(car).getBrand().getTitle(),
                        car.getModel().getTitle(),
                        car.getTransmissionType().toString(),
                        car.getMileage()));
                var vin = new Span("Vin: " + car.getVin());


                mainLay.add(new VerticalLayout(link, carName, vin));
                return mainLay;
            }));
            adsLayout.add(userAds);
        }

        add(infoLayout, adsLayout);
    }

    private Image createImage(Resource resourse, PhotoBasePayload firstPhoto) {
        var photo = new Image(new StreamResource(resourse.getFilename() == null ?
                "image" + firstPhoto.getPhotoId() : resourse.getFilename(),
                (InputStreamFactory) () -> {
                    try {
                        return resourse.getInputStream();
                    } catch (IOException e) {
                        log.error("error get ingupStream from resourses", e);
                        return null;
                    }
                }), "photo car");
        photo.setMaxWidth("250px");
        photo.setHeight("180px");
        return photo;
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
