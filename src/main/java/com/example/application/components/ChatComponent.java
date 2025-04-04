package com.example.application.components;


import com.example.application.clients.sellauto.payloads.ChatDetailsPayload;
import com.example.application.clients.sellauto.payloads.MessageBasePayload;
import com.example.application.clients.sellauto.payloads.ProfilePayload;
import com.example.application.clients.sellauto.payloads.UserBasePayload;
import com.example.application.util.JsonUtil;
import com.example.application.views.profile.UserProfileView;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouterLink;

import java.time.format.DateTimeFormatter;
import java.util.List;

@JsModule("./src/websocket-client.js")
public class ChatComponent extends HorizontalLayout {

    private final List<MessageBasePayload> messages;
    private final VirtualList<MessageBasePayload> listMessage;
    private final ChatDetailsPayload chat;

    public ChatComponent(Long chatId, String jwtToken,
                         String serverUrl,
                         List<MessageBasePayload> messages,
                         ChatDetailsPayload chat, ProfilePayload currentUser) {
        this.messages = messages.reversed();
        this.chat = chat;
        setWidthFull();

        listMessage = createListMessages(this.messages, currentUser);

        var membersCurrentChat = new VirtualList<UserBasePayload>();
        membersCurrentChat.setItems(chat.getUsers());
        membersCurrentChat.setSizeFull();
        membersCurrentChat.setRenderer(new ComponentRenderer<>(member ->
                new RouterLink(member.getFirstName(), UserProfileView.class, member.getUserId().toString())));
        var titleMembers = new H3("Участники");
        titleMembers.setWidthFull();
        var membersLay = new VerticalLayout(titleMembers, membersCurrentChat);
        membersLay.setWidth("20%");

        add(listMessage, membersLay);

        getElement().executeJs(
                """
                                WebSocketService.init($0, $1, $2, $3);
                        """,
                chatId.toString(),
                jwtToken,
                serverUrl + "/ws",
                getElement()
        );
    }

    private VirtualList<MessageBasePayload> createListMessages(List<MessageBasePayload> messages, ProfilePayload currentUser) {
        var listMessage = new VirtualList<MessageBasePayload>();
        listMessage.setItems(messages);
        listMessage.setRenderer(new ComponentRenderer<>(mesg -> {
            Div messageDiv = new Div();
            messageDiv.setClassName("chat-message");

            Div header = new Div();
            header.setClassName("message-header");

            Span name = new Span(mesg.getSenderName());
            name.setClassName("sender-name");

            Span time = new Span(mesg.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            time.setClassName("message-time");

            header.add(name, time);

            Span text = new Span(mesg.getMessage());
            text.setClassName("message-text");

            messageDiv.add(header, text);
            messageDiv.setWidth("45%");

            var messageLay = new VerticalLayout(messageDiv);
            messageLay.setWidthFull();

            var curUser = chat.getUsers()
                    .stream()
                    .filter(u -> currentUser.getUserId().equals(u.getUserId()))
                    .findFirst()
                    .orElse(null);

            if (curUser != null && curUser.getFirstName().equals(mesg.getSenderName())) {
                messageLay.setDefaultHorizontalComponentAlignment(Alignment.END);
                messageDiv.addClassName("my-message");
            } else {
                messageLay.setDefaultHorizontalComponentAlignment(Alignment.START);
            }

            return messageLay;
        }));
        listMessage.scrollToEnd();
        return listMessage;
    }

    @ClientCallable
    public void handleWebSocketMessage(String messageJson) {
        MessageBasePayload message = JsonUtil.readFromJson(messageJson, MessageBasePayload.class);
        UI.getCurrent().access(() -> {
            messages.add(message);
            listMessage.setItems(messages);
            listMessage.scrollToEnd();
        });
    }
}
