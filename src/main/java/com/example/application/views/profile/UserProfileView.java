package com.example.application.views.profile;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.clients.sellauto.payloads.FeedBackPayload;
import com.example.application.clients.sellauto.payloads.NewFeedBackPayload;
import com.example.application.clients.sellauto.payloads.ProfilePayload;
import com.example.application.components.FeedBackComment;
import com.example.application.components.RatingComponent;
import com.example.application.enums.Role;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.util.ComponentRenders;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


@Route(value = "user", layout = MainLayout.class)
@PageTitle("Пользователь")
@Slf4j
public class UserProfileView extends HorizontalLayout implements HasUrlParameter<String> {

    private final SellAutoRestClient sellAutoRestClient;

    private final VerticalLayout infoLayout;
    private final VerticalLayout adsLayout;
    private final Span rating;
    private final VirtualList<FeedBackPayload> feedbackLay;
    private final H3 feedbackTitle;
    private List<FeedBackPayload> feedbacks = new ArrayList<>();


    public UserProfileView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        rating = new Span();

        infoLayout = new VerticalLayout();
        infoLayout.setClassName("custom-block");
        infoLayout.setWidth("40%");
        infoLayout.setHeight("1300px");
        adsLayout = new VerticalLayout();
        adsLayout.setWidth("60%");
        feedbackLay = new VirtualList<>();
        feedbackLay.setWidthFull();
        infoLayout.setClassName("custom-block");
        adsLayout.setClassName("custom-block");
        feedbackTitle = new H3();
        feedbackTitle.setWidthFull();

        add(infoLayout, adsLayout);
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, String s) {
        infoLayout.removeAll();
        adsLayout.removeAll();
        var userId = Long.parseLong(s);
        var user = sellAutoRestClient.getProfileById(userId);
        var currentUser = sellAutoRestClient.getProfile();
        feedbacks = new ArrayList<>(sellAutoRestClient.getUserFeedBack(userId).getFeedbacks()
                .stream()
                .sorted((o1, o2) ->
                        o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList());

        var title = new H3(user.getFirstName());
        title.setWidthFull();
        var firstName = new Span("Имя: " + user.getFirstName());
        var lastName = new Span("Фамилия: " + user.getLastName());
        var numberPhone = new Span("Номер телефона: " + user.getAccount().getPhoneNumber());
        var email = new Span("Email: " + user.getAccount().getEmail());
        var countAds = new Span("Объявлений создано:  %d".formatted(user.getAds().size()));
        rating.setText("Рейтинг: %.1f ⭐".formatted(user.getRating() == null ? 0.0 : user.getRating()));
        infoLayout.add(title, firstName, lastName, numberPhone, email, countAds, rating);

        if (currentUser.getAccount().getRole() == Role.ROLE_ADMIN) {
            var blockedButton = createBlocedUserButton(user);
            infoLayout.add(blockedButton);
        }

        feedbackTitle.setText("Отзывы (%d)".formatted(feedbacks.size()));
        var ratingComponent = new RatingComponent();
        var comment = new TextArea();
        comment.setWidthFull();
        comment.setPlaceholder("Введите комментарий...");
        var sendButton = createSendFeedBackButton(ratingComponent, comment, currentUser, user);

        feedbackLay.setSizeFull();
        feedbackLay.setItems(feedbacks);
        feedbackLay.setRenderer(new ComponentRenderer<>(f ->
                new FeedBackComment(f, currentUser, sellAutoRestClient)));

        infoLayout.add(
                feedbackTitle,
                new H4("Оставить отзыв"),
                ratingComponent,
                comment,
                sendButton,
                new H4("Отзывы пользователей"),
                feedbackLay
        );
        var titleAds = new H3("Объявления");
        titleAds.setWidthFull();
        adsLayout.add(titleAds);

        var userAds = new VirtualList<AdPayload>();
        userAds.setWidth("95%");
        userAds.setItems(sellAutoRestClient.getUserAdsDetails(user.getUserId()).getAds());
        if (!user.getAds().isEmpty()) {
            userAds.setRenderer(generateAdRender(currentUser, userAds, user));
            adsLayout.add(userAds);
        }

    }

    private Button createSendFeedBackButton(RatingComponent ratingComponent, TextArea comment, ProfilePayload currentUser, ProfilePayload user) {
        return new Button("Отправить", e -> {
            if (ratingComponent.getRating() == 0) {
                Notification.show("Выберите оценку!", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            if (currentUser.getUserId().equals(user.getUserId())) {
                Notification.show("Нельзя поставить отзыв себе!", 5000, Notification.Position.TOP_CENTER);
                return;
            }
            try {
                var feedback = sellAutoRestClient.sendFeedBack(NewFeedBackPayload.builder()
                        .receiverId(user.getUserId())
                        .text(comment.getValue())
                        .estimation(ratingComponent.getRating())
                        .build());

                feedbacks.add(feedback);
                comment.clear();
                feedbackTitle.setText("Отзывы (%d)".formatted(feedbacks.size()));

                feedbackLay.setItems(feedbacks);

                Notification.show("Отзыв отправлен", 5000, Notification.Position.TOP_CENTER);
            } catch (SellAutoApiException ex) {
                log.error("error send feedback", ex);
                Notification.show("Ошибка при отправке отзыва...");
            }

        });
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
