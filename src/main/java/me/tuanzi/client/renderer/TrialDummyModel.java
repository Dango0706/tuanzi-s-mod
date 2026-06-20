package me.tuanzi.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.armorstand.ArmorStandModel;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class TrialDummyModel extends ArmorStandModel {
    public static final ModelLayerLocation TRIAL_DUMMY_LAYER = 
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(me.tuanzi.Tuanzis_mod.MOD_ID, "trial_dummy"), "main");

    private final ModelPart bottomPost;
    private final ModelPart customRightBodyStick;
    private final ModelPart customLeftBodyStick;
    private final ModelPart customShoulderStick;
    private final ModelPart customBasePlate;

    public TrialDummyModel(ModelPart root) {
        super(root);
        this.bottomPost = root.getChild("bottom_post");
        this.customRightBodyStick = root.getChild("right_body_stick");
        this.customLeftBodyStick = root.getChild("left_body_stick");
        this.customShoulderStick = root.getChild("shoulder_stick");
        this.customBasePlate = root.getChild("base_plate");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = net.minecraft.client.model.HumanoidModel.createMesh(
            net.minecraft.client.model.geom.builders.CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // 1. head（面袋头 + 脖子系绳 + 头后结绳）
        root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 8.0F, 8.0F)      // 8x8x8 愤怒面具头
            .texOffs(24, 16).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F)    // 3x1x3 脖子系绳
            .texOffs(24, 16).addBox(-1.0F, -7.0F, 4.0F, 2.0F, 4.0F, 2.0F),    // 2x4x2 面具结绳
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // 2. body（金黄稻草躯干 + 左右肩甲 + 打靶木胸垫 + 肩膀贯穿横木）
        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(4, 14).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F)     // 8x12x6 主躯干
            .texOffs(14, 16).addBox(0.5F, 1.0F, -4.0F, 3.0F, 10.0F, 1.0F)     // 3x10x1 左半靶牌
            .texOffs(17, 16).addBox(-3.5F, 1.0F, -4.0F, 3.0F, 10.0F, 1.0F)    // 3x10x1 右半靶牌
            .texOffs(0, 16).addBox(4.0F, 0.0F, -3.5F, 2.0F, 3.0F, 7.0F)       // 左肩甲
            .texOffs(0, 16).addBox(-6.0F, 0.0F, -3.5F, 2.0F, 3.0F, 7.0F)      // 右肩甲
            .texOffs(0, 16).addBox(-9.0F, 2.0F, -1.0F, 18.0F, 2.0F, 2.0F),    // 肩膀横木
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // 3. left_arm（左打击木桩臂）
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),     // 2x8x2 左臂木桩
            PartPose.offset(9.0F, 3.0F, 0.0F));

        // 4. right_arm（右打击木桩臂）
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),     // 2x8x2 右臂木桩
            PartPose.offset(-9.0F, 3.0F, 0.0F));

        // 5. bottom_post（铁箍立柱 + 十字阶梯八角木底座）
        root.addOrReplaceChild("bottom_post", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)     // 立柱桩
            .texOffs(0, 24).addBox(-2.5F, 10.0F, -2.5F, 5.0F, 2.0F, 5.0F)     // 下铁箍
            .texOffs(0, 24).addBox(-2.5F, 2.0F, -2.5F, 5.0F, 2.0F, 5.0F)      // 上铁箍
            .texOffs(0, 20).addBox(-6.0F, 13.0F, -2.0F, 12.0F, 1.0F, 4.0F)    // 12x1x4 底层十字 A
            .texOffs(0, 20).addBox(-2.0F, 13.0F, -6.0F, 4.0F, 1.0F, 12.0F)    // 4x1x12 底层十字 B
            .texOffs(0, 20).addBox(-4.0F, 12.0F, -2.0F, 8.0F, 1.0F, 4.0F)     // 8x1x4 中层十字 A
            .texOffs(0, 20).addBox(-2.0F, 12.0F, -4.0F, 4.0F, 1.0F, 8.0F),    // 4x1x8 中层十字 B
            PartPose.offset(0.0F, 12.0F, 0.0F));

        // 以下为 ArmorStandModel 构造链所需的空虚拟部件（隐藏，但必须存在）
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_body_stick", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_body_stick", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("shoulder_stick", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("base_plate", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(ArmorStandRenderState state) {
        super.setupAnim(state);
        // 让我们的 3D 模型组件保持可见
        this.body.visible = true;
        this.leftArm.visible = true;
        this.rightArm.visible = true;
        if (this.bottomPost != null) this.bottomPost.visible = true;

        // 隐藏不必要的虚拟及原版骨骼
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        if (this.customRightBodyStick != null) this.customRightBodyStick.visible = false;
        if (this.customLeftBodyStick != null) this.customLeftBodyStick.visible = false;
        if (this.customShoulderStick != null) this.customShoulderStick.visible = false;
        if (this.customBasePlate != null) this.customBasePlate.visible = false;
    }
}
