package com.example.application.views.chat;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.ChatDetailsPayload;
import com.example.application.components.ChatComponent;
import com.example.application.views.ads.Ad;
import com.example.application.views.util.ComponentRenders;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Route("chat")
@Slf4j
public class OpenChatView extends VerticalLayout implements HasUrlParameter<String> {

    private final SellAutoRestClient sellAutoRestClient;
    private final String serverUrl;


    private final TextArea messageField = new TextArea();

    public OpenChatView(SellAutoRestClient sellAutoRestClient, @Value("${sell-auto.base-url}") String serverUrl) {
        this.sellAutoRestClient = sellAutoRestClient;
        this.serverUrl = serverUrl;
        messageField.setWidthFull();
        messageField.setPlaceholder("Введите сообщение....");
    }


    @Override
    public void setParameter(BeforeEvent beforeEvent, String s) {
        try {
            final String jwtToken = sellAutoRestClient.getCurrentLogin() == null ? "" : sellAutoRestClient.getCurrentLogin().getAccessToken();
            Long chatId = Long.parseLong(s);
            var chat = sellAutoRestClient.getChat(chatId);
            var messages = sellAutoRestClient.getChatMessages(chatId);
            var currentUser = sellAutoRestClient.getProfile();
            var chatComponent = new ChatComponent(chatId, jwtToken, serverUrl, messages.getMessages(), chat, currentUser);
            var linkAd = createLinkAd(chat);

            add(linkAd, chatComponent);

            var sendMessageLay = createSendMessageButton(chatId, jwtToken);
            add(sendMessageLay);
        } catch (Exception e) {
            log.error(e.getMessage());
            Notification.show("Что-то пошло не так....", 5000, Notification.Position.TOP_CENTER);
        }
    }

    private HorizontalLayout createLinkAd(ChatDetailsPayload chat) {
        var ad = chat.getAd();
        var car = ad.getCar();
        var hor = new HorizontalLayout();
        hor.setWidthFull();
        hor.setClassName("custom-block");
        var link = new RouterLink(
                "%s %s %s  (%d ₽)"
                        .formatted(
                                car.getBrand().getTitle(),
                                car.getModel().getTitle(),
                                car.getYear(),
                                ad.getPrices().getLast().getPrice().intValue()),
                Ad.class,
                ad.getAdId().toString()
        );
        try {

            var image = ComponentRenders.createImage(
                    sellAutoRestClient.getPhoto(car.getPhotos().getFirst().getPhotoId()),
                    car.getPhotos().getFirst(),
                    "50px",
                    "25px");

            hor.add(image);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        hor.add(link);
        return hor;
    }

    private HorizontalLayout createSendMessageButton(Long chatId, String jwtToken) {
        Button sendButton = new Button("Отправить", e -> {
            try {
                getElement().executeJs(
                        "WebSocketService.sendMessage($0, $1, $2)",
                        chatId.toString(),
                        messageField.getValue(),
                        jwtToken
                );
                messageField.clear();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                Notification.show("Ошибка отправки сообщения", 5000, Notification.Position.TOP_CENTER);
            }
        });
        var sendMessageLay = new HorizontalLayout(messageField, sendButton);
        sendMessageLay.setWidth("78%");
        sendButton.setHeight("80px");
        return sendMessageLay;
    }
}
