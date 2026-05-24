package me.tuanzi.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.armorstand.ArmorStandModel;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;

@Environment(EnvType.CLIENT)
public class ScarecrowModel extends ArmorStandModel {
    private final ModelPart customRightBodyStick;
    private final ModelPart customLeftBodyStick;
    private final ModelPart customShoulderStick;
    private final ModelPart customBasePlate;

    public ScarecrowModel(ModelPart root) {
        super(root);
        this.customRightBodyStick = root.getChild("right_body_stick");
        this.customLeftBodyStick = root.getChild("left_body_stick");
        this.customShoulderStick = root.getChild("shoulder_stick");
        this.customBasePlate = root.getChild("base_plate");
    }

    @Override
    public void setupAnim(ArmorStandRenderState state) {
        super.setupAnim(state);
        // 将南瓜头向上微调，使其正好与干草块顶面重叠对齐（模型坐标系中 Y 轴向下为正）
        this.head.y = -1.0F;
        // 隐藏原本盔甲架的胸腔躯木棍与肩部横木（已由 3D 干草块方块饱满包裹）
        this.body.visible = false;
        if (this.customRightBodyStick != null) this.customRightBodyStick.visible = false;
        if (this.customLeftBodyStick != null) this.customLeftBodyStick.visible = false;
        if (this.customShoulderStick != null) this.customShoulderStick.visible = false;

        // 隐藏原版双腿木柱与石质底座，我们将以 3D 橡木栅栏单桩木柱直插地底，提供极其传统的稻草人底座
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        if (this.customBasePlate != null) this.customBasePlate.visible = false;
    }
}
