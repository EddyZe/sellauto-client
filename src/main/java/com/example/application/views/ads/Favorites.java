package com.example.application.views.ads;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.util.ComponentRenders;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;


@Slf4j
@PageTitle("Избранное")
@Route(value = "ads/favorite", layout = MainLayout.class)
@Menu(order = 0, icon = LineAwesomeIconUrl.HEART)
public class Favorites extends HorizontalLayout {

    private final SellAutoRestClient sellAutoRestClient;

    public Favorites(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setSizeFull();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        try {
            var ads = createAdList();
            ads.setSizeFull();

            add(ads);
        } catch (SellAutoApiException e) {
            Notification.show("Ошибка загрузки объявлений.", 5000, Notification.Position.TOP_CENTER);
            log.error(e.getMessage());
        }

    }

    private VirtualList<AdPayload> createAdList() {
        var ads = new VirtualList<AdPayload>();
        var currentUser = sellAutoRestClient.getProfile();
        ads.setItems(sellAutoRestClient.getFavorites(currentUser.getUserId()));
        ads.setRenderer(
                new ComponentRenderer<>(ad -> {
                    var photoId = ad.getCar().getPhotos().getFirst().getPhotoId();
                    var photoRes = sellAutoRestClient.getPhoto(photoId);
                    return ComponentRenders.generateComponentAdList(ad, photoRes, "290px", "175px");
                })
        );
        ads.setClassName("custom-block");
        return ads;
    }
}
