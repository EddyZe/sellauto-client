import SockJS from 'sockjs-client';
import Stomp from 'stompjs';


window.WebSocketService = {
    stompClient: null,

    init: function (chatId, jwtToken, serverWebSocketAddr, element) {
        const socket = new SockJS(serverWebSocketAddr);
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect(
            {'Authorization': 'Bearer ' + jwtToken},
            () => {
                this.stompClient.subscribe(
                    `/topic/chat/${chatId}/messages`,
                    (message) => {
                        try {
                            element.$server.handleWebSocketMessage(message.body)
                        } catch (e) {
                            console.error("Ошибка парсинга сообщения:", e);
                        }
                    }
                );
            },
            (error) => {
                console.error("Ошибка подключения:", error);
            }
        );
    },

    sendMessage: function (chatId, message, jwtToken) {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.send(
                `/app/chat/${chatId}/${jwtToken}`,
                {'Authorization': 'Bearer ' + jwtToken},
                message
            );
        } else {
            console.error("WebSocket connection not established");
        }
    }
};

