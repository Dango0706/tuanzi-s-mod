package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.tuanzi.entity.TrialDummyEntity;
import me.tuanzi.entity.DamageText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class TrialDummyRenderer extends LivingEntityRenderer<TrialDummyEntity, TrialDummyRenderState, ArmorStandArmorModel> {
    public static final Identifier TRIAL_DUMMY_SKIN_LOCATION = Identifier.fromNamespaceAndPath(me.tuanzi.Tuanzis_mod.MOD_ID, "textures/entity/trial_dummy.png");
    private final ArmorStandArmorModel bigModel = this.getModel();
    private final ArmorStandArmorModel smallModel;
    
    public TrialDummyRenderer(final EntityRendererProvider.Context context) {
        super(context, new TrialDummyModel(context.bakeLayer(TrialDummyModel.TRIAL_DUMMY_LAYER)), 0.0F);
        this.smallModel = new TrialDummyModel(context.bakeLayer(TrialDummyModel.TRIAL_DUMMY_LAYER));
        
        // 使用 Raw Type 强转，大师级避开 Java 泛型不协变的编译硬伤
        this.addLayer((net.minecraft.client.renderer.entity.layers.RenderLayer) new HumanoidArmorLayer(
            (net.minecraft.client.renderer.entity.RenderLayerParent) this,
            ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, context.getModelSet(), ArmorStandArmorModel::new),
            ArmorModelSet.bake(ModelLayers.ARMOR_STAND_SMALL_ARMOR, context.getModelSet(), ArmorStandArmorModel::new),
            context.getEquipmentRenderer()
        ));
        this.addLayer((net.minecraft.client.renderer.entity.layers.RenderLayer) new ItemInHandLayer((net.minecraft.client.renderer.entity.RenderLayerParent) this));
        this.addLayer((net.minecraft.client.renderer.entity.layers.RenderLayer) new CustomHeadLayer((net.minecraft.client.renderer.entity.RenderLayerParent) this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public Identifier getTextureLocation(final TrialDummyRenderState state) {
        return TRIAL_DUMMY_SKIN_LOCATION;
    }

    @Override
    public TrialDummyRenderState createRenderState() {
        return new TrialDummyRenderState();
    }

    @Override
    public void extractRenderState(final TrialDummyEntity entity, final TrialDummyRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        
        // 调用原版的人类肢体/盔甲/武器提取工具类，完美且一劳永逸地处理所有装备层面的状态同步
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);

        state.yRot = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.isMarker = false;
        state.isSmall = false;
        state.showArms = true;
        state.showBasePlate = false;
        
        // 锁定呈两侧水平展开手臂 (T-Pose 传统稻草人动作)
        state.bodyPose = new net.minecraft.core.Rotations(0.0F, 0.0F, 0.0F);
        state.headPose = new net.minecraft.core.Rotations(0.0F, 0.0F, 0.0F);
        state.leftArmPose = new net.minecraft.core.Rotations(0.0F, 0.0F, -90.0F);
        state.rightArmPose = new net.minecraft.core.Rotations(0.0F, 0.0F, 90.0F);
        state.leftLegPose = new net.minecraft.core.Rotations(0.0F, 0.0F, 0.0F);
        state.rightLegPose = new net.minecraft.core.Rotations(0.0F, 0.0F, 0.0F);
        
        // 受击晃动
        state.wiggle = entity.hurtTime > 0 ? (float)entity.hurtTime + partialTicks : 0.0F;

        // 客户端伤害悬浮字提取
        state.damageTexts.clear();
        state.damageTexts.addAll(entity.clientDamageTexts);

    }

    @Override
    public void submit(final TrialDummyRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        this.model = state.isSmall ? this.smallModel : this.bigModel;
        super.submit(state, poseStack, submitNodeCollector, camera);

        // 3. 遍历伤害文字列表，将其原生渲染至假人上方 3D 空间
        for (DamageText text : state.damageTexts) {
            poseStack.pushPose();
            poseStack.translate(text.xOffset, 2.0F + text.yOffset, text.zOffset);
            
            Component tagComponent = Component.literal(String.format("%.1f", text.damage))
                    .withStyle(net.minecraft.ChatFormatting.GOLD);
            
            submitNodeCollector.submitNameTag(
                poseStack,
                Vec3.ZERO,
                0,
                tagComponent,
                true,
                state.lightCoords,
                state.distanceToCameraSq,
                camera
            );
            poseStack.popPose();
        }
    }

    @Override
    protected void setupRotations(final TrialDummyRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        if (state.wiggle > 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(state.wiggle / 1.5F * (float) Math.PI) * 4.0F));
        }
    }
}
