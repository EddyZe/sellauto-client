package com.example.application.components;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RatingComponent extends HorizontalLayout {
    private int rating = 0;
    private final List<Button> stars = new ArrayList<>();

    public RatingComponent() {
        for (int i = 1; i <= 5; i++) {
            Button star = new Button();
            star.setIcon(VaadinIcon.STAR.create());
            star.addThemeVariants(ButtonVariant.LUMO_ICON);
            int finalI = i;
            star.addClickListener(e -> setRating(finalI));
            stars.add(star);
            add(star);
        }
        updateStars();
    }

    private void updateStars() {
        for (int i = 0; i < stars.size(); i++) {
            Button star = stars.get(i);
            if (i < rating) {
                star.setIcon(VaadinIcon.STAR.create());
                star.getStyle().set("color", "#ffd700"); // Золотой цвет
            } else {
                star.setIcon(VaadinIcon.STAR_O.create());
                star.getStyle().set("color", "#cccccc"); // Серый цвет
            }
        }
    }

    public void setRating(int rating) {
        this.rating = rating;
        updateStars();
    }
}
