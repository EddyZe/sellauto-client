package com.example.application.views;

import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.views.ads.AdListView;
import com.example.application.views.chat.ChatView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
@CssImport(value= "./themes/sellauto-client/styles.css", themeFor = "vaadin-app-layout")
public class MainLayout extends AppLayout {

    private final SellAutoRestClient sellAutoRestClient;

    private H1 viewTitle;

    public MainLayout(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE, "color-text");

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        Span appName = new Span("Sell Auto");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(appName);
        header.addClassNames("header-main");

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        var currentLogin = sellAutoRestClient.getCurrentLogin();

        nav.addItem(new SideNavItem("Объявления", AdListView.class,
                VaadinIcon.LIST.create()));

        if (currentLogin != null) {
            nav.addItem(new SideNavItem("Профиль", "profile",
                    VaadinIcon.USER.create()));
            nav.addItem(new SideNavItem("Создать объявление", "ads/create",
                    VaadinIcon.PLUS.create()));
            nav.addItem(new SideNavItem("Мои объявления", "ads/my",
                    VaadinIcon.LIST.create()));
            nav.addItem(new SideNavItem("Избранные объявления", "ads/favorite",
                    VaadinIcon.HEART.create()));
            nav.addItem(new SideNavItem("Чаты", ChatView.class,
                    VaadinIcon.MAILBOX.create()));
        } else {
            nav.addItem(new SideNavItem("Вход / Регистрация", "login",
                    VaadinIcon.SIGN_IN.create()));
        }

        return nav;
    }

    private Footer createFooter() {
        return new Footer();
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }
}
