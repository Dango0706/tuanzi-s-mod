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
public class ScarecrowModel extends ArmorStandModel {
    public static final ModelLayerLocation SCARECROW_LAYER = 
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(me.tuanzi.Tuanzis_mod.MOD_ID, "scarecrow"), "main");

    private final ModelPart bottomPost;
    private final ModelPart customRightBodyStick;
    private final ModelPart customLeftBodyStick;
    private final ModelPart customShoulderStick;
    private final ModelPart customBasePlate;

    public ScarecrowModel(ModelPart root) {
        super(root);
        this.bottomPost = root.getChild("bottom_post");
        this.customRightBodyStick = root.getChild("right_body_stick");
        this.customLeftBodyStick = root.getChild("left_body_stick");
        this.customShoulderStick = root.getChild("shoulder_stick");
        this.customBasePlate = root.getChild("base_plate");
    }

    public static LayerDefinition createBodyLayer() {
        // 必须以 HumanoidModel.createMesh() 为基底，确保父类构造链（HumanoidModel→ArmorStandArmorModel→ArmorStandModel）
        // 所需的 hat、right_leg、left_leg 等所有标准子部件均存在，避免 getChild() 抛出 NoSuchElementException
        MeshDefinition mesh = net.minecraft.client.model.HumanoidModel.createMesh(
            net.minecraft.client.model.geom.builders.CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // 1. head（南瓜头，带帽子与南瓜柄）— 32x32 纹理，多立方体拼合
        root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)      // 南瓜头主体
            .texOffs(0, 16).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 2.0F, 2.0F)   // 南瓜柄
            .texOffs(8, 16).addBox(-6.0F, -9.0F, -6.0F, 12.0F, 1.0F, 12.0F)  // 草帽沿
            .texOffs(8, 16).addBox(-3.0F, -11.0F, -3.0F, 6.0F, 2.0F, 6.0F),  // 草帽顶
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // 2. body（干草块身体与腰带）— 纹理偏移 (8, 16)，8×12×4 立体干草卷
        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(8, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)    // 立体干草块身体
            .texOffs(8, 16).addBox(-4.5F, 6.0F, -2.5F, 9.0F, 1.0F, 5.0F),    // 绑绳腰带
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // 3. left_arm（左侧垂直木棍与碎草手掌）— 纹理偏移 (0, 20) / (8, 16)
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
            .texOffs(0, 20).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F)       // 左臂木棍（垂直向下）
            .texOffs(8, 16).addBox(-0.5F, 8.0F, -1.5F, 3.0F, 3.0F, 3.0F),    // 左手碎稻草（悬吊）
            PartPose.offset(4.0F, 4.0F, 0.0F));

        // 4. right_arm（右侧垂直木棍与碎草手掌）— 纹理偏移 (0, 20) / (8, 16)
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
            .texOffs(0, 20).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F)      // 右臂木棍（垂直向下）
            .texOffs(8, 16).addBox(-2.5F, 8.0F, -1.5F, 3.0F, 3.0F, 3.0F),     // 右手碎稻草（悬吊）
            PartPose.offset(-4.0F, 4.0F, 0.0F));

        // 5. bottom_post（底部支撑木桩）— 纹理偏移 (0, 20)
        root.addOrReplaceChild("bottom_post", CubeListBuilder.create()
            .texOffs(0, 20).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),   // 支撑木柱
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
