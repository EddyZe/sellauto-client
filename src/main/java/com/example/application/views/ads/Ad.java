package com.example.application.views.ads;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.clients.sellauto.payloads.PhotoBasePayload;
import com.example.application.clients.sellauto.payloads.PriceBasePayload;
import com.example.application.enums.Role;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.profile.UserProfileView;
import com.example.application.views.util.ComponentRenders;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Route(value = "/ads", layout = MainLayout.class)
public class Ad extends VerticalLayout implements HasUrlParameter<String> {

    private final SellAutoRestClient sellAutoRestClient;

    public Ad(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setWidthFull();
    }


    @Override
    public void setParameter(BeforeEvent beforeEvent, String param) {
        try {
            var adLay = new VerticalLayout();
            adLay.setWidthFull();
            adLay.setClassName("custom-block");

            var id = Long.parseLong(param);
            var ad = sellAutoRestClient.getAdId(id);

            var car = ad.getCar();

            var title = new H3("%s %s, %s  (%d ₽)".formatted(
                    car.getBrand().getTitle(),
                    car.getModel().getTitle(),
                    car.getYear(),
                    ad.getPrices().getLast().getPrice().intValue())
            );
            title.setWidthFull();

            var carInfo = new VerticalLayout();
            var images = generatePhotoLayout(ad);
            carInfo.setWidth("30%");
            var horizontalLayout = new HorizontalLayout(carInfo, images);
            horizontalLayout.setWidthFull();
            var user = ad.getUser();

            var sellerLay = new HorizontalLayout(new Span("Продавец: "));

            var userLink = new RouterLink(
                    "%s (%.1f ⭐)".formatted(user.getFirstName(), user.getRating() == null ? 0. : user.getRating()),
                    UserProfileView.class, user.getUserId().toString()
            );
            sellerLay.setWidthFull();
            sellerLay.add(userLink);

            carInfo.add(sellerLay);
            carInfo.add(new Span("Пробег: %d км".formatted(car.getMileage())));
            carInfo.add(new Span("Бренд: %s".formatted(car.getBrand().getTitle())));
            carInfo.add(new Span("Модель: %s".formatted(car.getModel().getTitle())));
            carInfo.add(new Span("Год: %s".formatted(car.getYear())));
            carInfo.add(new Span("Кузов: %s".formatted(car.getBodyType().toString())));
            carInfo.add(new Span("Привод: %s".formatted(car.getDrive().toString())));
            carInfo.add(new Span("Коробка: %s".formatted(car.getTransmissionType().toString())));
            carInfo.add(new Span("VIN: %s".formatted(car.getVin())));

            var commentAndSendMessageLay = createCommentAndSendMessageLayout(ad);

            var chart = new ChartView(ad.getPrices()
                    .stream()
                    .map(PriceBasePayload::getPrice)
                    .toList());
            var chartLay = new VerticalLayout(chart);
            chartLay.setWidthFull();
            chartLay.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

            var comment = new Div(ad.getDescription());
            comment.setSizeFull();

            adLay.add(title,
                    horizontalLayout,
                    new H4("График изменения цен. Текущая цена: %.2f ₽".formatted(ad.getPrices().getFirst().getPrice())),
                    chartLay,
                    new H4("Комментарий от продавца"),
                    comment);

            try {
                var currentUser = sellAutoRestClient.getProfile();
                if (currentUser == null) {
                    adLay.add(commentAndSendMessageLay);
                    return;
                }

                if (currentUser.getUserId().equals(ad.getUser().getUserId()) || currentUser.getAccount().getRole() == Role.ROLE_ADMIN) {
                    var editButton = new Button("Редактировать", e ->
                            UI.getCurrent().navigate("/ads/edit/" + ad.getAdId()));
                    var deleteButton = createDeleteButton(ad);

                    var buttonLay = new HorizontalLayout(editButton, deleteButton);

                    if (currentUser.getAccount().getRole() == Role.ROLE_ADMIN) {
                        adLay.add(commentAndSendMessageLay);
                    }
                    adLay.add(buttonLay);
                } else
                    adLay.add(commentAndSendMessageLay);

            } catch (Exception ignored) {
            }

            add(adLay);
        } catch (NumberFormatException e) {
            log.error("Error: {}", e.getMessage());
            Notification.show("Произошла ошибка...", 5000, Notification.Position.TOP_CENTER);
        }
    }

    private Button createDeleteButton(AdPayload ad) {
        return new Button("Удалить", e -> {
            try {
                sellAutoRestClient.deleteAd(ad.getAdId());
                UI.getCurrent().navigate(UserAdsView.class);
                Notification.show("Объявление удалено", 5000, Notification.Position.TOP_CENTER);
            } catch (SellAutoApiException exe) {
                log.error("error delete ad", exe);
                Notification.show("Ошибка удаления", 5000, Notification.Position.TOP_CENTER);
            }
        });
    }

    private VerticalLayout createCommentAndSendMessageLayout(AdPayload ad) {
        return new VerticalLayout(
                new Button("Написать продавцу", e -> {
                    try {
                        var currentUser = sellAutoRestClient.getProfile();
                        if (ad.getUser().getUserId().equals(currentUser.getUserId())) {
                            Notification.show("Вы не можете написать самому себе.", 5000, Notification.Position.TOP_CENTER);
                            return;
                        }

                        var chat = sellAutoRestClient.openChat(ad.getAdId());
                        UI.getCurrent().navigate("chat/" + chat.getChatId());
                    } catch (Exception ex) {
                        log.error("error creating chat", ex);
                        Notification.show("Что-то пошло не так.... Попробуйте повторить позже", 5000, Notification.Position.TOP_CENTER);
                    }
                }));
    }

    private VerticalLayout generatePhotoLayout(AdPayload ad) {
        var photos = ad.getCar().getPhotos()
                .stream()
                .map(this::createImage)
                .toList();

        var mainPhoto = photos.getFirst();
        mainPhoto.setMaxWidth("500px");
        mainPhoto.setMaxHeight("300px");
        var photosLay = new HorizontalLayout();
        photosLay.setWidth("90%");
        for (int i = 1; i < photos.size(); i++) {
            var img = photos.get(i);
            img.setMaxHeight("90px");
            img.setMaxWidth("150px");
            photosLay.add(photos.get(i));
        }
        var adPhotos = new VerticalLayout(mainPhoto, photosLay);
        adPhotos.setWidthFull();
        adPhotos.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        return adPhotos;
    }

    private Image createImage(PhotoBasePayload p) {
        try {
            var res = sellAutoRestClient.getPhoto(p.getPhotoId());
            var img = ComponentRenders.createImage(res, p, "100%", "100%");
            img.addClickListener(e -> {
                var dialog = new Dialog();
                dialog.add(ComponentRenders.createImage(res, p, "900px", "450px"));
                Button closeButton = new Button(new Icon("lumo", "cross"),
                        (ev) -> dialog.close());
                closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                dialog.getHeader().add(closeButton);
                dialog.open();
            });
            img.setClassName("custom-card");

            return img;
        } catch (Exception e) {
            log.error("error get image: {}", e.getMessage());
            return null;
        }
    }

}
