package com.serverPresentation;

import com.serverPresentation.facades.ServerStartupFacade;

public class ServerApplication {
    public static void main(String[] args) {
        ServerStartupFacade facade = new ServerStartupFacade();
        facade.start();
    }
}