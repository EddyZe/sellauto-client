package com.example.application.configs;

import com.example.application.handlers.GlobalErrorHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VaadinConfig implements VaadinServiceInitListener {

    private final GlobalErrorHandler errorHandler;

    public VaadinConfig(GlobalErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(e ->
                e.getSession().setErrorHandler(errorHandler)
        );
    }
}
