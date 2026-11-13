package me.tuanzi.client.renderer.model;

import me.tuanzi.Tuanzis_mod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/**
 * 蓝图大炮权威运作模型（严格 100% 对齐 blueprint_cannon.bbmodel）
 * 包含：
 * - base: 固定稳固基座（铸铁底盘、四角防滑加固角铁、梯形过渡台阶与环形回转导轨，高度 0~6.5 像素）
 * - turret: 回转机身（回转承重盘、左右立柱耳架、大型旋转轴承端盖与后方减震气动斜撑，随偏航角水平旋转）
 * - barrel: 俯仰主炮组（主旋转转轴、-Z 重型加压闭锁后仓、气动弹仓、+Z 主炮身过渡段、方形延伸炮管与深邃炮口套）
 */
@Environment(EnvType.CLIENT)
public class BlueprintCannonModel extends Model<Unit> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_cannon"), "main");

    public final ModelPart base;
    public final ModelPart turret;
    public final ModelPart barrel;

    public BlueprintCannonModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.base = root.getChild("base");
        this.turret = root.getChild("turret");
        this.barrel = this.turret.getChild("barrel");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. base（固定底盘：Pivot 设在地面中心，高度 0~6.5 像素）
        root.addOrReplaceChild("base",
                CubeListBuilder.create()
                        // foundation: 宽16×高3×长16 (高度 0~3 像素)
                        .texOffs(0, 0).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 3.0F, 16.0F)
                        // 4 个防滑包角角铁 (高度 0~4 像素)
                        .texOffs(0, 0).addBox(-8.0F, 0.0F, -8.0F, 3.0F, 4.0F, 3.0F) // corner_nw
                        .texOffs(0, 0).addBox(5.0F, 0.0F, -8.0F, 3.0F, 4.0F, 3.0F)  // corner_ne
                        .texOffs(0, 0).addBox(-8.0F, 0.0F, 5.0F, 3.0F, 4.0F, 3.0F)  // corner_sw
                        .texOffs(0, 0).addBox(5.0F, 0.0F, 5.0F, 3.0F, 4.0F, 3.0F)   // corner_se
                        // pedestal: 宽13×高2×长13 (高度 3~5 像素)
                        .texOffs(0, 0).addBox(-6.5F, 3.0F, -6.5F, 13.0F, 2.0F, 13.0F)
                        // ring_rail: 宽11×高1.5×长11 (高度 5~6.5 像素)
                        .texOffs(0, 0).addBox(-5.5F, 5.0F, -5.5F, 11.0F, 1.5F, 11.0F),
                PartPose.ZERO);

        // 2. turret（回转机身：Pivot 位于底座承重转盘中心 Y=6.5 像素）
        PartDefinition turret = root.addOrReplaceChild("turret",
                CubeListBuilder.create()
                        // swivel_plate: 底盘回转承重盘 (宽10×厚2×长10，高度 6.5~8.5 像素，相对 pivot 为 0~2)
                        .texOffs(0, 0).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 2.0F, 10.0F)
                        // left_bracket: 左耳立柱支架 (宽2.5×高5.5×长6，高度 8.5~14 像素，相对 pivot 为 2.0~7.5)
                        .texOffs(0, 0).addBox(-5.0F, 2.0F, -3.0F, 2.5F, 5.5F, 6.0F)
                        // right_bracket: 右耳立柱支架
                        .texOffs(0, 0).addBox(2.5F, 2.0F, -3.0F, 2.5F, 5.5F, 6.0F)
                        // left_pivot_cap: 左侧主旋转轴承端盖 (凸起齿轮结构，高度 10.5~12.5，相对 pivot 为 4.0~6.0)
                        .texOffs(0, 0).addBox(-5.8F, 4.0F, -1.5F, 0.8F, 2.0F, 3.0F)
                        // right_pivot_cap: 右侧主旋转轴承端盖
                        .texOffs(0, 0).addBox(5.0F, 4.0F, -1.5F, 0.8F, 2.0F, 3.0F)
                        // left_strut / right_strut: 后方减震气动斜撑 (严格位于 -Z 炮尾后方，高度 8.5~11，相对 pivot 为 2.0~4.5)
                        .texOffs(0, 0).addBox(-4.5F, 2.0F, -4.5F, 1.5F, 2.5F, 1.5F)
                        .texOffs(0, 0).addBox(3.0F, 2.0F, -4.5F, 1.5F, 2.5F, 1.5F),
                PartPose.offset(0.0F, 6.5F, 0.0F));

        // 3. barrel（俯仰主炮组：Pivot 位于两耳轴承中心 Y=11.5 像素处，相对 turret 抬高 5 个像素）
        // 关键约束：严格遵循 Blockbench 真实朝向——炮口指向 +Z，炮尾闭锁后舱位于 -Z！
        turret.addOrReplaceChild("barrel",
                CubeListBuilder.create()
                        // main_shaft: 主旋转横轴 (贯穿左右耳架，高度 10.5~12.5，相对 pivot 为 -1.0~1.0)
                        .texOffs(0, 0).addBox(-3.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F)
                        // cannon_breech: 重型闭锁后舱 (严格向 -Z 延伸，高度 9~14，相对 pivot 为 -2.5~2.5)
                        .texOffs(0, 0).addBox(-2.5F, -2.5F, -6.0F, 5.0F, 5.0F, 6.0F)
                        // pneumatic_chamber: 顶部气动加压弹仓 / 蓝图输入仓 (位于 -Z 后舱上方，高度 14~15.5，相对 pivot 为 2.5~4.0)
                        .texOffs(0, 0).addBox(-1.5F, 2.5F, -4.5F, 3.0F, 1.5F, 3.0F)
                        // cannon_body: 炮身过渡段 (向 +Z 延伸，高度 9.5~13.5，相对 pivot 为 -2.0~2.0)
                        .texOffs(0, 0).addBox(-2.5F, -2.0F, 0.0F, 5.0F, 4.0F, 5.0F)
                        // cannon_barrel: 方形延伸炮管 (向 +Z 延伸，高度 10~13，相对 pivot 为 -1.5~1.5)
                        .texOffs(0, 0).addBox(-2.0F, -1.5F, 5.0F, 4.0F, 3.0F, 4.0F)
                        // 加固炮口套（上、下、左、右四片围筑出深邃 3x2 内膛，端面精准位于 Z = +14.0F）
                        .texOffs(0, 0).addBox(-2.5F, 1.0F, 9.0F, 5.0F, 1.0F, 5.0F)    // 上膛沿
                        .texOffs(0, 0).addBox(-2.5F, -2.0F, 9.0F, 5.0F, 1.0F, 5.0F)  // 下膛沿
                        .texOffs(0, 0).addBox(-2.5F, -1.0F, 9.0F, 1.0F, 2.0F, 5.0F)   // 左膛沿
                        .texOffs(0, 0).addBox(1.5F, -1.0F, 9.0F, 1.0F, 2.0F, 5.0F),   // 右膛沿
                PartPose.offset(0.0F, 5.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
