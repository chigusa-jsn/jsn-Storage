package com.chigusa.jsnstorage.client;

import com.chigusa.jsnstorage.Net;
import com.chigusa.jsnstorage.menu.StorageMenu;
import com.chigusa.jsnstorage.storage.Tier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

/** 入れ物の画面（絵は使わず、四角でかく） */
public class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final String[] SORTS = {"種類", "名前", "数"};
    private int sortMode;

    public StorageScreen(StorageMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = menu.width;
        this.imageHeight = menu.height;
        this.inventoryLabelX = menu.invX;
        this.inventoryLabelY = menu.invY - 11;
        this.titleLabelX = menu.storageX;
    }

    @Override
    protected void init() {
        super.init();
        Button b = addRenderableWidget(Button.builder(Component.literal("⇅"), btn -> PacketDistributor.sendToServer(new Net.Sort(sortMode)))
                .bounds(leftPos + imageWidth - 22, topPos + 4, 14, 12).build());
        b.setTooltip(Tooltip.create(Component.literal("並べ替え")));
        Button m = addRenderableWidget(Button.builder(Component.literal(SORTS[sortMode]), btn -> {
            sortMode = (sortMode + 1) % SORTS.length;
            btn.setMessage(Component.literal(SORTS[sortMode]));
        }).bounds(leftPos + imageWidth - 52, topPos + 4, 28, 12).build());
        m.setTooltip(Tooltip.create(Component.literal("並べ替えの順番（クリックで切りかえ）")));
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        Tier t = menu.tier;
        int edge = t.color;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E24);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        // ランクの色のふち
        g.fill(x, y, x + imageWidth, y + 2, edge);
        g.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, edge);
        g.fill(x, y, x + 2, y + imageHeight, edge);
        g.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, edge);
        // アップグレードの列
        g.fill(x + 4, y + menu.storageY - 4, x + 28, y + menu.storageY + t.upgrades * 22, 0xFF8B8B8B);
        for (Slot s : menu.slots) {
            int sx = x + s.x - 1, sy = y + s.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }
        // アップグレードの枠には印
        for (int j = 0; j < t.upgrades; j++) {
            Slot s = menu.slots.get(t.slots + j);
            if (!s.hasItem()) g.drawCenteredString(font, "§8＋", x + s.x + 8, y + s.y + 4, 0xFFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, 6, 0xFF404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        // アップグレードの枠の説明
        for (int j = 0; j < menu.tier.upgrades; j++) {
            Slot s = menu.slots.get(menu.tier.slots + j);
            if (!s.hasItem() && isHovering(s.x, s.y, 16, 16, mx, my))
                g.renderTooltip(font, Component.literal("アップグレードを入れる枠"), mx, my);
        }
    }
}
