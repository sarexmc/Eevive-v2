package com.revivalcampfire;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEvents {
    public static void init(IEventBus bus) {
        bus.addListener(ClientEvents::clientSetup);
        bus.addListener(ClientEvents::overlays);
        MinecraftForge.EVENT_BUS.addListener(ClientRitual::onClientTick);
    }

    private static void clientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> MenuScreens.register(RevivalCampfireMod.REVIVE_MENU.get(), ReviveScreen::new));
    }

    private static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("ritual_flash", ClientRitual::renderFlash);
    }
}
