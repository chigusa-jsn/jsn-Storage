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
import com.chigusa.jsnstorage.upgrade.UpgradeData;
import com.chigusa.jsnstorage.upgrade.UpgradeItem;
import com.chigusa.jsnstorage.upgrade.UpgradeNet;
import com.chigusa.jsnstorage.upgrade.UpgradeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 入れ物の画面（絵は使わず、四角でかく） */
public class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final String[] SORTS = {"種類", "名前", "数"};
    private int sortMode;
    /** 設定を開いているアップグレード（-1 なら閉じている） */
    private int panel = -1;
    private record Btn(int x, int y, int w, int h, String label, Runnable run) {}
    private final List<Btn> buttons = new ArrayList<>();
    private static final int PW = 9 * 18 + 14;

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

    // ================= アップグレードの設定パネル =================
    private int px() {
        int x = leftPos + imageWidth + 4;
        return x + PW > width ? Math.max(2, leftPos - PW - 4) : x;
    }

    private int py() {
        return topPos;
    }

    private ItemStack panelStack() {
        if (panel < 0 || panel >= menu.tier.upgrades) return ItemStack.EMPTY;
        return menu.slots.get(menu.tier.slots + panel).getItem();
    }

    private int panelHeight(UpgradeItem u) {
        int fs = u.type.filterSize(u.rank);
        int h = 40 + font.split(Component.literal(u.type.desc), PW - 12).size() * 9;
        if (fs > 0) h += ((fs + 8) / 9) * 18 + 16;
        if (u.type.capacity(u.rank) > 0) h += 60;
        return h;
    }

    private void send(int action, int value, String item) {
        PacketDistributor.sendToServer(new UpgradeNet.Action(panel, action, value, item));
    }

    private void renderPanel(GuiGraphics g, int mx, int my) {
        buttons.clear();
        ItemStack st = panelStack();
        if (!(st.getItem() instanceof UpgradeItem u)) {
            panel = -1;
            return;
        }
        int x = px(), y = py(), h = panelHeight(u);
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(x, y, x + PW, y + h, 0xF0202028);
        g.renderOutline(x, y, PW, h, menu.tier.color);
        g.renderItem(st, x + 6, y + 5);
        g.drawString(font, "§l" + st.getHoverName().getString(), x + 26, y + 9, 0xFFFFFF);
        int yy = y + 26;
        for (var l : font.split(Component.literal("§7" + u.type.desc), PW - 12)) {
            g.drawString(font, l, x + 6, yy, 0xFFFFFF);
            yy += 9;
        }
        yy += 4;
        int fs = u.type.filterSize(u.rank);
        Component tip = null;
        if (fs > 0) {
            g.drawString(font, "§b登録（アイテムを持って枠をクリック）", x + 6, yy, 0xFFFFFF);
            yy += 11;
            List<String> f = UpgradeData.filter(st);
            for (int i = 0; i < fs; i++) {
                int cx = x + 7 + (i % 9) * 18, cy = yy + (i / 9) * 18;
                g.fill(cx, cy, cx + 17, cy + 17, 0xFF101018);
                g.renderOutline(cx, cy, 17, 17, 0xFF505060);
                Item it = i < f.size() ? UpgradeData.item(f.get(i)) : null;
                if (it != null) g.renderItem(new ItemStack(it), cx + 1, cy + 1);
                int idx = i;
                buttons.add(new Btn(cx, cy, 17, 17, "", () -> {
                    ItemStack c = menu.getCarried();
                    send(UpgradeNet.SET_FILTER, idx, c.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(c.getItem()).toString());
                }));
                if (mx >= cx && mx < cx + 17 && my >= cy && my < cy + 17)
                    tip = it != null ? Component.literal(new ItemStack(it).getHoverName().getString() + "§8（空の手でクリックで消す）") : Component.literal("§8アイテムを持ってクリックで登録");
            }
            yy += ((fs + 8) / 9) * 18 + 4;
        }
        double cap = u.type.capacity(u.rank);
        if (cap > 0) {
            double stored = UpgradeData.stored(st);
            String unit = u.type == UpgradeType.XP ? "ポイント" : "";
            g.drawString(font, "§eためている量：§f" + (int) stored + " / " + (int) cap + unit, x + 6, yy, 0xFFFFFF);
            yy += 10;
            g.fill(x + 6, yy, x + PW - 6, yy + 4, 0xFF101018);
            int col = u.type == UpgradeType.XP ? 0xFF80F040 : u.type == UpgradeType.MANA ? 0xFF4080F0 : 0xFFF0C030;
            g.fill(x + 6, yy, x + 6 + (int) ((PW - 12) * Math.min(1, stored / cap)), yy + 4, col);
            yy += 9;
            int bw = (PW - 16) / 2;
            if (u.type == UpgradeType.XP) {
                buttons.add(new Btn(x + 6, yy, bw, 14, "1レベル預ける", () -> send(UpgradeNet.XP_IN, 1, "")));
                buttons.add(new Btn(x + 10 + bw, yy, bw, 14, "1レベル出す", () -> send(UpgradeNet.XP_OUT, 1, "")));
                buttons.add(new Btn(x + 6, yy + 17, bw, 14, "全部預ける", () -> send(UpgradeNet.XP_IN, -1, "")));
                buttons.add(new Btn(x + 10 + bw, yy + 17, bw, 14, "全部出す", () -> send(UpgradeNet.XP_OUT, -1, "")));
            } else {
                buttons.add(new Btn(x + 6, yy, bw, 14, "いまの分を預ける", () -> send(UpgradeNet.BATTERY_IN, 0, "")));
                buttons.add(new Btn(x + 10 + bw, yy, bw, 14, "満タンまで出す", () -> send(UpgradeNet.BATTERY_OUT, 0, "")));
            }
        }
        for (Btn b : buttons) {
            if (b.label().isEmpty()) continue;
            boolean hov = mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h();
            g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), hov ? 0xFF5A5A70 : 0xFF3A3A4A);
            g.renderOutline(b.x(), b.y(), b.w(), b.h(), 0xFF707088);
            g.drawCenteredString(font, b.label(), b.x() + b.w() / 2, b.y() + 3, 0xFFFFFF);
        }
        g.pose().popPose();
        if (tip != null) g.renderTooltip(font, tip, mx, my);
    }

    private boolean inPanel(double mx, double my) {
        if (panel < 0 || !(panelStack().getItem() instanceof UpgradeItem u)) return false;
        return mx >= px() && mx < px() + PW && my >= py() && my < py() + panelHeight(u);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inPanel(mx, my)) {
            for (Btn b : buttons)
                if (mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h()) {
                    b.run().run();
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    return true;
                }
            return true;
        }
        // アップグレードの枠を右クリック：設定を開く
        if (button == 1) for (int j = 0; j < menu.tier.upgrades; j++) {
            Slot sl = menu.slots.get(menu.tier.slots + j);
            if (sl.hasItem() && isHovering(sl.x, sl.y, 16, 16, mx, my)) {
                panel = panel == j ? -1 : j;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        return !inPanel(mx, my) && super.hasClickedOutside(mx, my, left, top, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderPanel(g, mx, my);
        renderTooltip(g, mx, my);
        // アップグレードの枠の説明
        for (int j = 0; j < menu.tier.upgrades; j++) {
            Slot s = menu.slots.get(menu.tier.slots + j);
            if (!s.hasItem() && isHovering(s.x, s.y, 16, 16, mx, my))
                g.renderTooltip(font, Component.literal("アップグレードを入れる枠（入れたら右クリックで設定）"), mx, my);
        }
    }
}
