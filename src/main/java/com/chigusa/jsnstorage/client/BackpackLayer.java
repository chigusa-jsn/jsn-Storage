package com.chigusa.jsnstorage.client;

import com.chigusa.jsnstorage.JsnStorage;
import com.chigusa.jsnstorage.storage.BackpackItem;
import com.chigusa.jsnstorage.storage.Tier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;

/** 背負ったリュックを、体の後ろに立体で描く */
public final class BackpackLayer {
    public static ModelResourceLocation wornModel(Tier t) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(JsnStorage.MODID, "item/" + t.id + "_backpack_worn"));
    }

    public static void render(ItemStack stack, LivingEntity entity, EntityModel<?> parent, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!(stack.getItem() instanceof BackpackItem b)) return;
        pose.pushPose();
        if (parent instanceof HumanoidModel<?> hm) {
            hm.body.translateAndRotate(pose);
        }
        // 体の空間：y は下向き、背中は +z。鎧の上に来るように少し離す
        pose.translate(0, 15 / 16f, 2.6 / 16f);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.translate(-0.5, 0, 0);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(wornModel(b.tier));
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()),
                null, model, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        pose.popPose();
    }

    private BackpackLayer() {}
}
