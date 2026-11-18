package fr.minevegas.casino;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public record CasinoPayoutTarget(String owner, double x, double z) {
   public static final CasinoPayoutTarget EMPTY = new CasinoPayoutTarget("", (double)0.0F, (double)0.0F);
   public static final Codec<CasinoPayoutTarget> CODEC = RecordCodecBuilder.<CasinoPayoutTarget>create((instance) -> instance.group(Codec.STRING.optionalFieldOf("owner", "").forGetter(CasinoPayoutTarget::owner), Codec.DOUBLE.optionalFieldOf("x", (double)0.0F).forGetter(CasinoPayoutTarget::x), Codec.DOUBLE.optionalFieldOf("z", (double)0.0F).forGetter(CasinoPayoutTarget::z)).apply(instance, CasinoPayoutTarget::new));

   public CasinoPayoutTarget(String owner, double x, double z) {
      if (owner == null) {
         owner = "";
      }

      try {
         if (!owner.isEmpty()) {
            UUID.fromString(owner);
         }
      } catch (IllegalArgumentException var7) {
         owner = "";
      }

      if (!Double.isFinite(x) || !Double.isFinite(z)) {
         owner = "";
         x = (double)0.0F;
         z = (double)0.0F;
      }

      this.owner = owner;
      this.x = x;
      this.z = z;
   }

   public static CasinoPayoutTarget of(Player player) {
      return player == null ? EMPTY : new CasinoPayoutTarget(player.getUUID().toString(), player.getX(), player.getZ());
   }

   public void aim(ServerLevel level, ItemEntity item) {
      if (!this.owner.isEmpty()) {
         UUID id = UUID.fromString(this.owner);
         Player player = level.getPlayerByUUID(id);
         double targetX = player != null && player.isAlive() ? player.getX() : this.x;
         double targetZ = player != null && player.isAlive() ? player.getZ() : this.z;
         double dx = targetX - item.getX();
         double dz = targetZ - item.getZ();
         double length = Math.hypot(dx, dz);
         Vec3 velocity = item.getDeltaMovement();
         double speed = Math.hypot(velocity.x, velocity.z);
         item.setTarget(id);
         item.setDeltaMovement(length > 1.0E-7 ? dx / length * speed : (double)0.0F, velocity.y, length > 1.0E-7 ? dz / length * speed : (double)0.0F);
      }
   }
}
