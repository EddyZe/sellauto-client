package com.example.application.views.chat;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.ChatDetailsPayload;
import com.example.application.clients.sellauto.payloads.ChatsDetailsPayload;
import com.example.application.clients.sellauto.payloads.MessageBasePayload;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.util.ComponentRenders;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@PageTitle("Чаты")
@Route(value = "chats/my", layout = MainLayout.class)
public class ChatView extends HorizontalLayout {

    private final SellAutoRestClient sellAutoRestClient;

    private final VerticalLayout chatListBlock;

    public ChatView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setSizeFull();

        chatListBlock = new VerticalLayout();
        chatListBlock.setWidthFull();
        init();
        add(chatListBlock);
    }

    private void init() {
        try {
            var chats = sellAutoRestClient.getMyChats();
            var openChat = new VirtualList<MessageBasePayload>();
            var chatList = createListChats(chats, openChat);


            chatListBlock.add(chatList);
        } catch (SellAutoApiException e) {
            log.error("SellAutoRestClient exception", e);
        }
    }

    private VirtualList<ChatDetailsPayload> createListChats(ChatsDetailsPayload chats, VirtualList<MessageBasePayload> openChat) {
        var chatList = new VirtualList<ChatDetailsPayload>();
        chatList.setItems(chats.getChats());
        chatList.setRenderer(new ComponentRenderer<>(chat -> {
            var ad = chat.getAd();
            var car = ad.getCar();
            var photoPayload = car.getPhotos().getFirst();
            var photoRes = sellAutoRestClient.getPhoto(photoPayload.getPhotoId());
            var photo = ComponentRenders.createImage(photoRes, photoPayload, "60px", "40px");
            var titleChat = "%s %s %s (%d ₽)".formatted(
                    car.getBrand().getTitle(),
                    car.getModel().getTitle(),
                    car.getYear(),
                    ad.getPrices().getLast().getPrice().intValue()
            );


            var link = new RouterLink(titleChat, OpenChatView.class, chat.getChatId().toString());
            var chatView = new HorizontalLayout(photo, link);
            chatView.setWidth("90%");
            chatView.setClassName("custom-card");

            chatView.addClickListener(event -> openChat.setItems(chat.getMessages()));
            return chatView;
        }));
        return chatList;
    }

}
