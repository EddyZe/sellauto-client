package com.example.application.components;

import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.FeedBackPayload;
import com.example.application.clients.sellauto.payloads.ProfilePayload;
import com.example.application.enums.Role;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.profile.UserProfileView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;


@Slf4j
public class FeedBackComment extends VerticalLayout {


    public FeedBackComment(FeedBackPayload feedback, ProfilePayload currentUser, SellAutoRestClient sellAutoRestClient) {
        var feedBackLay = new VerticalLayout();
        feedBackLay.setWidth("95%");
        feedBackLay.setHeightFull();
        feedBackLay.setClassName("feedback");
        var sender = feedback.getSender();
        var fullNameSender = new RouterLink(
                "%s %s".formatted(sender.getFirstName(), sender.getLastName()),
                UserProfileView.class,
                sender.getUserId().toString()
        );
        var estimation = new Span(feedback.getEstimation().intValue() + " ⭐");
        var comment = new Span(feedback.getText() == null ? "" : feedback.getText());
        comment.setSizeFull();
        var createdAt = new Span(feedback.getCreatedAt() == null ? "" :
                feedback.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        createdAt.setClassName("feedback-time");
        createdAt.setWidthFull();

        feedBackLay.add(new HorizontalLayout(estimation, fullNameSender), comment, createdAt);

        if (currentUser.getUserId().equals(feedback.getSender().getUserId()) || currentUser.getAccount().getRole() == Role.ROLE_ADMIN) {
            var deleteButton = createDeleteFeedBackButton(feedback, sellAutoRestClient);
            deleteButton.setWidthFull();

            feedBackLay.add(deleteButton);
        }

        add(feedBackLay);
    }

    private Button createDeleteFeedBackButton(FeedBackPayload feedback, SellAutoRestClient sellAutoRestClient) {
        return new Button("Удалить", e -> {
            try {
                sellAutoRestClient.deleteFeedBack(feedback.getId());
                Notification.show("Отзыв удален", 5000, Notification.Position.TOP_CENTER);
                UI.getCurrent().getPage().reload();
            } catch (SellAutoApiException ex) {
                Notification.show("Ошибка при удалении отзыва", 5000, Notification.Position.TOP_CENTER);
                log.error("error deleting feedback", ex);
            }
        });
    }
}
