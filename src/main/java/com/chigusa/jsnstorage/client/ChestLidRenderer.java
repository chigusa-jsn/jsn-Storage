package com.chigusa.jsnstorage.client;

import com.chigusa.jsnstorage.JsnStorage;
import com.chigusa.jsnstorage.storage.Kind;
import com.chigusa.jsnstorage.storage.StorageBlock;
import com.chigusa.jsnstorage.storage.StorageBlockEntity;
import com.chigusa.jsnstorage.storage.Tier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** チェストのふた。開いている人がいると、後ろを軸にパカッと開く */
public class ChestLidRenderer implements BlockEntityRenderer<StorageBlockEntity> {
    public ChestLidRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    public static ModelResourceLocation lidModel(Tier t) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(JsnStorage.MODID, "block/" + t.id + "_chest_lid"));
    }

    public static ModelResourceLocation shulkerLidModel(Tier t) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(JsnStorage.MODID, "block/" + t.id + "_shulker_box_lid"));
    }

    @Override
    public void render(StorageBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof StorageBlock sb) || sb.kind == Kind.BACKPACK) return;
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        float open = be.lid.getOpenness(partial);
        open = 1.0f - open;
        open = 1.0f - open * open * open;
        boolean shulker = sb.kind == Kind.SHULKER;
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(shulker ? shulkerLidModel(sb.tier) : lidModel(sb.tier));
        pose.pushPose();
        // 向き（モデルは北向きが基準）
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-yRot(facing)));
        pose.translate(-0.5, 0, -0.5);
        if (shulker) {
            // シュルカー：ふたが半ブロック持ち上がりながら、くるっと回る
            pose.translate(0.5, open * 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(open * 270f));
            pose.translate(-0.5, 0, -0.5);
        } else {
            // チェスト：後ろのふち（z=15/16, y=10/16）を軸に開く
            pose.translate(0, 10 / 16f, 15 / 16f);
            pose.mulPose(Axis.XP.rotationDegrees(open * 85f));
            pose.translate(0, -10 / 16f, -15 / 16f);
        }
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                state, model, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }

    private static float yRot(Direction d) {
        return switch (d) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
