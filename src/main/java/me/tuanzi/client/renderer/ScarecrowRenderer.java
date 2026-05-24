package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ScarecrowRenderer extends LivingEntityRenderer<ArmorStand, ArmorStandRenderState, ArmorStandArmorModel> {
    public static final Identifier DEFAULT_SKIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/armorstand/armorstand.png");
    private final ArmorStandArmorModel bigModel = this.getModel();
    private final ArmorStandArmorModel smallModel;
    
    // 通过实例级变量存储，完美绕开原版复杂的泛型等式约束，并且节省每次新建State的垃圾回收开销
    private final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
    private final MovingBlockRenderState fenceRenderState = new MovingBlockRenderState();

    public ScarecrowRenderer(final EntityRendererProvider.Context context) {
        super(context, new ScarecrowModel(context.bakeLayer(ModelLayers.ARMOR_STAND)), 0.0F);
        this.smallModel = new ScarecrowModel(context.bakeLayer(ModelLayers.ARMOR_STAND_SMALL));
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, context.getModelSet(), ArmorStandArmorModel::new),
                ArmorModelSet.bake(ModelLayers.ARMOR_STAND_SMALL_ARMOR, context.getModelSet(), ArmorStandArmorModel::new),
                context.getEquipmentRenderer()
            )
        );
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    public Identifier getTextureLocation(final ArmorStandRenderState state) {
        return DEFAULT_SKIN_LOCATION;
    }

    @Override
    public ArmorStandRenderState createRenderState() {
        return new ArmorStandRenderState();
    }

    @Override
    public void extractRenderState(final ArmorStand entity, final ArmorStandRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        net.minecraft.client.renderer.entity.HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
        state.yRot = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.isMarker = entity.isMarker();
        state.isSmall = entity.isSmall();
        state.showArms = entity.showArms();
        state.showBasePlate = entity.showBasePlate();
        state.bodyPose = entity.getBodyPose();
        state.headPose = entity.getHeadPose();
        state.leftArmPose = entity.getLeftArmPose();
        state.rightArmPose = entity.getRightArmPose();
        state.leftLegPose = entity.getLeftLegPose();
        state.rightLegPose = entity.getRightLegPose();
        state.wiggle = (float)(entity.level().getGameTime() - entity.lastHit) + partialTicks;

        // 提取干草块及底部橡木栅栏支撑方块的材质与光照参数，使其平滑融入周围环境
        BlockPos pos = entity.blockPosition();
        this.movingBlockRenderState.randomSeedPos = pos;
        this.movingBlockRenderState.blockPos = pos;
        this.movingBlockRenderState.blockState = net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState();
        
        this.fenceRenderState.randomSeedPos = pos;
        this.fenceRenderState.blockPos = pos;
        this.fenceRenderState.blockState = net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState();
        
        if (entity.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            this.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            this.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            this.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
            
            this.fenceRenderState.biome = clientLevel.getBiome(pos);
            this.fenceRenderState.cardinalLighting = clientLevel.cardinalLighting();
            this.fenceRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(final ArmorStandRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        this.model = state.isSmall ? this.smallModel : this.bigModel;
        super.submit(state, poseStack, submitNodeCollector, camera);

        // 1. 渲染包裹身体的干草块方块
        poseStack.pushPose();
        // 水平对齐中心，高度偏置向上平移至 0.65F（高度 [0.65, 1.65]）
        // 这样干草块顶端完美塞入南瓜头底端（~1.6F），且手臂（1.375F）极为自然地从干草块侧面中上部穿出！
        poseStack.translate(-0.5F, 0.65F, -0.5F);
        submitNodeCollector.submitMovingBlock(poseStack, this.movingBlockRenderState);
        poseStack.popPose();

        // 2. 渲染底部的中心橡木栅栏支撑木桩
        poseStack.pushPose();
        // 栅栏高度为 1.0，偏置平移至 -0.35F（高度 [-0.35, 0.65]），底端深插地底，顶端与干草块底部咬合，绝不浮空！
        poseStack.translate(-0.5F, -0.35F, -0.5F);
        submitNodeCollector.submitMovingBlock(poseStack, this.fenceRenderState);
        poseStack.popPose();
    }

    @Override
    protected void setupRotations(final ArmorStandRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        if (state.wiggle < 5.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(state.wiggle / 1.5F * (float) Math.PI) * 3.0F));
        }
    }

    @Override
    protected boolean shouldShowName(final ArmorStand entity, final double distanceToCameraSq) {
        return entity.isCustomNameVisible();
    }

    @Override
    @Nullable
    protected RenderType getRenderType(final ArmorStandRenderState state, final boolean isBodyVisible, final boolean forceTransparent, final boolean appearGlowing) {
        if (!state.isMarker) {
            return super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
        } else {
            Identifier texture = this.getTextureLocation(state);
            if (forceTransparent) {
                return RenderTypes.entityTranslucent(texture, false);
            } else {
                return isBodyVisible ? RenderTypes.entityCutout(texture, false) : null;
            }
        }
    }
}
