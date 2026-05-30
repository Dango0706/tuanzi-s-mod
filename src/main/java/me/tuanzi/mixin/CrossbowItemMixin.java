package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.SeekingArrowAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    @Inject(method = "createProjectile", at = @At("RETURN"))
    private void onCreateProjectile(Level level, LivingEntity shooter, ItemStack heldItem, ItemStack projectile, boolean isCrit, CallbackInfoReturnable<Projectile> cir) {
        Projectile projectileEntity = cir.getReturnValue();
        if (projectileEntity instanceof SeekingArrowAccessor seekingArrow && shooter instanceof Player player) {
            var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var seekingEnch = lookup.getOrThrow(ModEnchantments.SEEKING_ARROW);
            if (EnchantmentHelper.getItemEnchantmentLevel(seekingEnch, heldItem) > 0) {
                // 进行射线检测以在服务端确定目标
                double maxDistance = 64.0;
                Vec3 eyePos = player.getEyePosition(1.0f);
                Vec3 viewVec = player.getViewVector(1.0f);
                Vec3 endPos = eyePos.add(viewVec.scale(maxDistance));
                
                AABB searchArea = player.getBoundingBox().expandTowards(viewVec.scale(maxDistance)).inflate(1.0);
                EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                    player,
                    eyePos,
                    endPos,
                    searchArea,
                    entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable(),
                    maxDistance * maxDistance
                );
                
                if (entityHitResult != null) {
                    seekingArrow.tuanzis_mod$setSeekingTargetEntity(entityHitResult.getEntity());
                } else {
                    // 方块射线检测
                    BlockHitResult blockHitResult = level.clip(new net.minecraft.world.level.ClipContext(
                        eyePos,
                        endPos,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        player
                    ));
                    if (blockHitResult.getType() != HitResult.Type.MISS) {
                        seekingArrow.tuanzis_mod$setSeekingTargetPos(blockHitResult.getLocation());
                    } else {
                        seekingArrow.tuanzis_mod$setSeekingTargetPos(endPos);
                    }
                }
            }
        }
    }
}
