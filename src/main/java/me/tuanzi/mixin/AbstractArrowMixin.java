package me.tuanzi.mixin;

import me.tuanzi.util.SeekingArrowAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Entity implements SeekingArrowAccessor {

    @org.spongepowered.asm.mixin.Shadow
    protected abstract boolean isInGround();

    @Unique
    private Entity tuanzis_mod$seekingTargetEntity;
    @Unique
    private Vec3 tuanzis_mod$seekingTargetPos;
    @Unique
    private boolean tuanzis_mod$hasCorrectedTrajectory = false;
    @Unique
    private int tuanzis_mod$seekingTickCount = 0;

    protected AbstractArrowMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tuanzis_mod$setSeekingTargetEntity(Entity entity) {
        this.tuanzis_mod$seekingTargetEntity = entity;
    }

    @Override
    public Entity tuanzis_mod$getSeekingTargetEntity() {
        return this.tuanzis_mod$seekingTargetEntity;
    }

    @Override
    public void tuanzis_mod$setSeekingTargetPos(Vec3 pos) {
        this.tuanzis_mod$seekingTargetPos = pos;
    }

    @Override
    public Vec3 tuanzis_mod$getSeekingTargetPos() {
        return this.tuanzis_mod$seekingTargetPos;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.level().isClientSide()) {
            if (!this.tuanzis_mod$hasCorrectedTrajectory) {
                this.tuanzis_mod$seekingTickCount++;
                if (this.tuanzis_mod$seekingTickCount >= 10) {
                    this.tuanzis_mod$hasCorrectedTrajectory = true;
                }
            }
            return;
        }

        if (!this.tuanzis_mod$hasCorrectedTrajectory) {
            this.tuanzis_mod$seekingTickCount++;
            if (this.tuanzis_mod$seekingTickCount == 10) { // 射出后的第 10 个 tick (0.5秒)
                this.tuanzis_mod$hasCorrectedTrajectory = true;
                
                Vec3 currentVelocity = this.getDeltaMovement();
                if (currentVelocity.lengthSqr() < 1.0E-7) return;

                Vec3 predictedPos = null;
                if (this.tuanzis_mod$seekingTargetEntity != null && this.tuanzis_mod$seekingTargetEntity.isAlive()) {
                    // 直接获取目标实体高度除以 2，加在 position 坐标中作为最精确的追踪目标点
                    double halfHeight = this.tuanzis_mod$seekingTargetEntity.getBbHeight() * 0.5;
                    predictedPos = this.tuanzis_mod$seekingTargetEntity.position().add(0.0, halfHeight, 0.0);
                } else if (this.tuanzis_mod$seekingTargetPos != null) {
                    predictedPos = this.tuanzis_mod$seekingTargetPos;
                }

                if (predictedPos != null) {
                    Vec3 arrowPos = this.position();
                    Vec3 desiredDirection = predictedPos.subtract(arrowPos).normalize();
                    Vec3 currentDirection = currentVelocity.normalize();
                    
                    double dot = currentDirection.dot(desiredDirection);
                    double angleRad = Math.acos(net.minecraft.util.Mth.clamp(dot, -1.0, 1.0));
                    
                    Vec3 newVelocity;
                    double maxAngleRad = Math.PI / 6.0; // 30度
                    if (angleRad <= maxAngleRad) {
                        newVelocity = desiredDirection.scale(currentVelocity.length());
                    } else {
                        double t = maxAngleRad / angleRad;
                        Vec3 blended = currentDirection.scale(1.0 - t).add(desiredDirection.scale(t)).normalize();
                        newVelocity = blended.scale(currentVelocity.length());
                    }
                    
                    this.setDeltaMovement(newVelocity);
                    
                    net.minecraft.world.entity.Entity owner = ((AbstractArrow)(Object)this).getOwner();
                    String ownerName = owner != null ? owner.getName().getString() : "未知射手";
                    String targetName = this.tuanzis_mod$seekingTargetEntity != null ? this.tuanzis_mod$seekingTargetEntity.getName().getString() : "无实体目标";
                    double angleDeg = Math.toDegrees(angleRad);
                    me.tuanzi.util.ModLog.debug(ownerName, targetName, "追踪箭弹道修正成功：修正角度为 " + String.format("%.2f", angleDeg) + "°，修正后目标坐标为 " + predictedPos + "。");
                    
                    // 旋转同步
                    double d = newVelocity.x;
                    double e = newVelocity.y;
                    double f = newVelocity.z;
                    double g = Math.sqrt(d * d + f * f);
                    this.setYRot((float)(net.minecraft.util.Mth.atan2(d, f) * 180.0 / Math.PI));
                    this.setXRot((float)(net.minecraft.util.Mth.atan2(e, g) * 180.0 / Math.PI));
                    this.yRotO = this.getYRot();
                    this.xRotO = this.getXRot();

                    // 强行同步至客户端，更新视觉朝向
                    ((net.minecraft.world.entity.projectile.Projectile)(Object)this).needsSync = true;
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickReturn(CallbackInfo ci) {
        if (this.isInGround()) return;

        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() > 1.0E-7) {
            float yRot = (float)(net.minecraft.util.Mth.atan2(movement.x, movement.z) * 180.0F / (float)Math.PI);
            float xRot = (float)(net.minecraft.util.Mth.atan2(movement.y, movement.horizontalDistance()) * 180.0F / (float)Math.PI);
            
            // 计算当前渲染旋转与根据速度算出的真实旋转之间的度数差异
            float deltaY = net.minecraft.util.Mth.wrapDegrees(yRot - this.getYRot());
            float deltaX = net.minecraft.util.Mth.wrapDegrees(xRot - this.getXRot());
            
            // 如果度数偏差大于 10 度，说明发生了突兀的物理偏转（第 10 tick 修正或网络速度包延迟到达），此时直接绕过 lerpRotation 强制瞬间对齐
            if (Math.abs(deltaY) > 10.0F || Math.abs(deltaX) > 10.0F) {
                this.setYRot(yRot);
                this.setXRot(xRot);
                this.yRotO = yRot;
                this.xRotO = xRot;
            }
        }
    }
}
