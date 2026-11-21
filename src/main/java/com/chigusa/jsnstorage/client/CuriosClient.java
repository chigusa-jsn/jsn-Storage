package com.chigusa.jsnstorage.client;

import com.chigusa.jsnstorage.JsnStorage;
import com.chigusa.jsnstorage.storage.Tier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Curios が入っているときだけ読み込まれる：背中の枠のリュックを描く */
final class CuriosClient {
    static void register() {
        for (Tier t : Tier.values()) {
            CuriosRendererRegistry.register(JsnStorage.backpack(t), Renderer::new);
        }
    }

    static final class Renderer implements ICurioRenderer {
        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            ICurioRenderer.translateIfSneaking(pose, slotContext.entity());
            ICurioRenderer.rotateIfSneaking(pose, slotContext.entity());
            BackpackLayer.render(stack, slotContext.entity(), parent.getModel(), pose, buffers, light);
        }
    }

    private CuriosClient() {}
}
