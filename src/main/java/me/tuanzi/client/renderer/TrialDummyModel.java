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

        // 1. head（布袋头 + 脖子系绳）— 麻布材质与叉叉眼，32x32 纹理中的 (0, 0)
        root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 6.0F, 6.0F)      // 6x6x6 布袋头
            .texOffs(0, 0).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F),     // 3x1x3 脖系绳 (UV 共享)
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // 2. body（皮革皮垫躯干 + 肩部双向贯穿木棍）— 皮革底色与打靶同心圆
        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 10.0F, 6.0F)    // 8x10x6 丰满皮垫躯干
            .texOffs(0, 12).addBox(-8.0F, 8.0F, -1.0F, 16.0F, 2.0F, 2.0F),   // 16x2x2 肩膀木棍贯穿 (UV 共享木纹)
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // 3. left_arm（左手臂木棍）— 垂直木棍，挂在肩膀上
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
            .texOffs(0, 12).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),     // 2x8x2 左手臂木棍 (UV 共享木纹)
            PartPose.offset(4.0F, 4.0F, 0.0F));

        // 4. right_arm（右手臂木棍）— 垂直木棍，挂在肩膀上
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
            .texOffs(0, 12).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),    // 2x8x2 右手臂木棍 (UV 共享木纹)
            PartPose.offset(-4.0F, 4.0F, 0.0F));

        // 5. bottom_post（底部支撑木桩与十字底座）— 支撑假人屹立在地面
        root.addOrReplaceChild("bottom_post", CubeListBuilder.create()
            .texOffs(0, 12).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F)    // 3x10x3 支撑木柱
            .texOffs(0, 12).addBox(-8.0F, -10.0F, -2.0F, 16.0F, 2.0F, 4.0F)  // 16x2x4 底座横条1
            .texOffs(0, 12).addBox(-2.0F, -10.0F, -8.0F, 4.0F, 2.0F, 16.0F), // 4x2x16 底座横条2
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
