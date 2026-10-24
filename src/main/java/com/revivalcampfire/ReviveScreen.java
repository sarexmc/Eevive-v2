package com.revivalcampfire;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Menu de resurreccion: lista de baneados a la izquierda, fogata y boton "Revivir" a la derecha. */
public class ReviveScreen extends AbstractContainerScreen<ReviveMenu> {
    private static final int ROWS = 8, ROW_H = 14, LIST_W = 138, LIST_X = 10, LIST_Y = 26;

    private int scroll = 0;
    private int selected = -1;
    private Button reviveButton;

    public ReviveScreen(ReviveMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 168;
        this.titleLabelX = 10;
        this.titleLabelY = 8;
    }

    @Override
    protected void init() {
        super.init();
        reviveButton = Button.builder(Component.translatable("gui.revivalcampfire.revive"), b -> {
            if (selected >= 0 && selected < menu.banned.size()) {
                Network.CHANNEL.sendToServer(new Network.ReviveRequest(menu.pos, menu.banned.get(selected).id()));
                onClose();
            }
        }).bounds(leftPos + 164, topPos + 130, 74, 20).build();
        addRenderableWidget(reviveButton);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        reviveButton.active = selected >= 0 && selected < menu.banned.size();
        renderBackground(g);
        super.render(g, mx, my, pt);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        // marco de madera
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B1608);
        g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF9A6A35);
        g.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF7A5228);

        // lista (pergamino)
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        g.fill(lx - 2, ly - 2, lx + LIST_W + 2, ly + ROWS * ROW_H + 2, 0xFF3B2410);
        g.fill(lx, ly, lx + LIST_W, ly + ROWS * ROW_H, 0xFFE8D9B0);

        if (menu.banned.isEmpty()) {
            g.drawString(font, Component.translatable("gui.revivalcampfire.empty"), lx + 4, ly + 4, 0x6B4A2A, false);
        }
        for (int r = 0; r < ROWS; r++) {
            int idx = scroll + r;
            if (idx >= menu.banned.size()) break;
            int ry = ly + r * ROW_H;
            if (idx == selected) g.fill(lx, ry, lx + LIST_W, ry + ROW_H, 0xFFC9A66B);
            else if (mx >= lx && mx < lx + LIST_W && my >= ry && my < ry + ROW_H) g.fill(lx, ry, lx + LIST_W, ry + ROW_H, 0xFFDCC794);
            g.drawString(font, menu.banned.get(idx).name(), lx + 4, ry + 3, 0x2B1A0A, false);
        }
        // barra de scroll
        int total = menu.banned.size();
        if (total > ROWS) {
            int barH = ROWS * ROW_H;
            int h = Math.max(10, barH * ROWS / total);
            int y = ly + (barH - h) * scroll / (total - ROWS);
            g.fill(lx + LIST_W - 4, y, lx + LIST_W - 1, y + h, 0xFF6B4A2A);
        }

        // panel derecho con la fogata
        g.fill(leftPos + 156, topPos + 24, leftPos + imageWidth - 8, topPos + imageHeight - 8, 0xFF6A4522);
        g.fill(leftPos + 190, topPos + 40, leftPos + 222, topPos + 72, 0xFF3B2410);
        g.fill(leftPos + 191, topPos + 41, leftPos + 221, topPos + 71, 0xFF1D1A2B);
        g.pose().pushPose();
        g.pose().translate(leftPos + 190 + 16 - 12, topPos + 40 + 16 - 12, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.renderItem(new ItemStack(RevivalCampfireMod.CAMPFIRE_ITEM.get()), 0, 0);
        g.pose().popPose();

        if (selected >= 0 && selected < menu.banned.size()) {
            String n = menu.banned.get(selected).name();
            g.drawString(font, n, leftPos + 156 + (imageWidth - 164 - font.width(n)) / 2 + 4, topPos + 82, 0xFFF1D9, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFF1D9, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        if (mx >= lx && mx < lx + LIST_W && my >= ly && my < ly + ROWS * ROW_H) {
            int idx = scroll + (int) ((my - ly) / ROW_H);
            if (idx < menu.banned.size()) selected = idx;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, menu.banned.size() - ROWS);
        scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, max);
        return true;
    }
}
