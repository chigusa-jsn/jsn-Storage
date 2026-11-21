package com.chigusa.jsnstorage.client;

import com.chigusa.jsnstorage.JsnStorage;
import com.chigusa.jsnstorage.Net;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = JsnStorage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    public static final KeyMapping OPEN = new KeyMapping("key.jsn_storage.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.jsn_storage");

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent e) {
        e.register(JsnStorage.STORAGE_MENU.get(), StorageScreen::new);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(OPEN);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ev -> {
            while (OPEN.consumeClick()) if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null)
                PacketDistributor.sendToServer(new Net.OpenBackpack());
        });
    }

    private ClientSetup() {}
}
