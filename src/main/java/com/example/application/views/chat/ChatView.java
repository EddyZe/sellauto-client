package com.example.application.views.chat;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.ChatBasePayload;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@PageTitle("Чаты")
@Route(value = "chats/my", layout =  MainLayout.class)
public class ChatView extends VerticalLayout {

    private final SellAutoRestClient sellAutoRestClient;

    public ChatView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setSizeFull();

        init();
    }

    private void init() {
        try {
            var chats = sellAutoRestClient.getMyChats();
            var chatList = new VirtualList<ChatBasePayload>();
            chatList.setItems(chats.getChats());
            chatList.setRenderer(new ComponentRenderer<>(chat -> {
                return new Span(chat.getChatId().toString());
            }));

            add(chatList);
        } catch (SellAutoApiException e) {
            log.error("SellAutoRestClient exception", e);
        }
    }

}
