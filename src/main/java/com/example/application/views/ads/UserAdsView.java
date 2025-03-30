package com.example.application.views.ads;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.clients.sellauto.payloads.EditAdPayload;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.util.ComponentRenders;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@PageTitle("Мои объявления")
@Route(value = "ads/my", layout = MainLayout.class)
public class UserAdsView extends VerticalLayout {

    private final SellAutoRestClient sellAutoRestClient;

    public UserAdsView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setSizeFull();

        try {
            var profile = this.sellAutoRestClient.getProfile();
            var ads = new VirtualList<AdPayload>();
            ads.setHeightFull();
            ads.setWidth("70%");
            ads.setItems(this.sellAutoRestClient.getUserAdsDetails(profile.getUserId()).getAds());
            ads.setRenderer(
                    new ComponentRenderer<>(ad -> {
                        var photoId = ad.getCar().getPhotos().getFirst().getPhotoId();
                        var photoRes = sellAutoRestClient.getPhoto(photoId);
                        var adLay = ComponentRenders.generateComponentAdList(ad, photoRes, "280px", "155px");
                        adLay.setWidth("95%");
                        var editStatus = new Button(ad.getIsActive() ? "Изменить на статус продано" : "Изменить на статус продается");
                        var delete = new Button("Удалить", e -> {
                            try {
                                sellAutoRestClient.deleteAd(ad.getAdId());
                                ads.setItems(this.sellAutoRestClient.getUserAdsDetails(profile.getUserId()).getAds());
                                Notification.show("Объявление удалено", 5000, Notification.Position.TOP_CENTER);
                            } catch (SellAutoApiException ex) {
                                log.error("error deleting ad", ex);
                                Notification.show("Ошибка удаления объявления.");
                            }
                        });

                        editStatus.addClickListener(e -> {
                            try {
                                sellAutoRestClient.editAd(EditAdPayload.builder()
                                        .isActive(!ad.getIsActive())
                                        .build(), ad.getAdId());
                                ad.setIsActive(!ad.getIsActive());
                                ads.setItems(this.sellAutoRestClient.getUserAdsDetails(profile.getUserId()).getAds());
                                Notification.show("Статус изменен", 5000, Notification.Position.TOP_CENTER);
                            } catch (SellAutoApiException ex) {
                                log.error("error editing ad", ex);
                            }
                        });

                        adLay.add(new HorizontalLayout(editStatus, delete));
                        return adLay;
                    })
            );
            ads.setClassName("custom-block");

            add(ads);
        } catch (Exception e) {
            log.error(e.getMessage());
            Notification.show("Ошибка загрузки ваших объявлений", 5000, Notification.Position.TOP_CENTER);
        }
    }
}
