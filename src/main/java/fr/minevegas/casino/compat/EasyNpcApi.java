package fr.minevegas.casino.compat;

import fr.minevegas.casino.DealerAppearance;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class EasyNpcApi {
   public static final String PREFIX = "de.markusbordihn.easynpc.";
   private static final Class<?> NPC = find("de.markusbordihn.easynpc.entity.easynpc.EasyNPC");

   private EasyNpcApi() {
   }

   private static Class<?> find(String name) {
      try {
         return Class.forName(name, false, EasyNpcApi.class.getClassLoader());
      } catch (ClassNotFoundException var2) {
         return null;
      }
   }

   public static boolean available() {
      return NPC != null;
   }

   public static boolean isNpc(Entity entity) {
      return NPC != null && NPC.isInstance(entity);
   }

   public static Object call(Object receiver, String method, Class<?>[] signature, Object... args) throws ReflectiveOperationException {
      try {
         return receiver.getClass().getMethod(method, signature).invoke(receiver, args);
      } catch (InvocationTargetException failure) {
         Throwable var6 = failure.getCause();
         if (var6 instanceof ReflectiveOperationException reflection) {
            throw reflection;
         } else {
            throw new ReflectiveOperationException("Easy NPC " + method, failure.getCause());
         }
      }
   }

   public static Object call(Object receiver, String method) throws ReflectiveOperationException {
      return call(receiver, method, new Class[0]);
   }

   public static boolean canRecruit(Entity entity, Player player) throws ReflectiveOperationException {
      Object ownerData = call(entity, "getEasyNPCOwnerData");

      try {
         Object owner = call(ownerData, "getOwnerUUID");
         if (owner == null || owner.equals(new UUID(0L, 0L)) || owner.equals(player.getUUID())) {
            return true;
         }
      } catch (ReflectiveOperationException var5) {
      }

      Boolean hasOwner = (Boolean)call(ownerData, "hasNPCOwner");
      if (hasOwner != null && hasOwner) {
         Boolean isOwner = (Boolean)call(ownerData, "isNPCOwner", new Class[]{UUID.class}, player.getUUID());
         return Boolean.TRUE.equals(isOwner);
      } else {
         return true;
      }
   }

   public static DealerAppearance capture(Entity entity) throws ReflectiveOperationException {
      Object skinData = call(entity, "getEasyNPCSkinData");
      String model = ((Enum)call(skinData, "getSkinModel")).name();
      if (!model.equals("HUMANOID") && !model.equals("HUMANOID_SLIM") && !model.equals("VILLAGER")) {
         return DealerAppearance.EMPTY;
      } else {
         Object entry = call(skinData, "getSkinDataEntry");
         CompoundTag skin = (CompoundTag)call(entry, "createTag");
         if (skin.toString().length() > 262144) {
            throw new ReflectiveOperationException("NPC skin data exceeds the badge limit");
         } else {
            Object variantData = call(entity, "getEasyNPCVariantData");
            String variant = ((Enum)call(variantData, "getSkinVariantType")).name();
            return new DealerAppearance(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), model, variant, skin, entity.getName().getString());
         }
      }
   }

   public static void apply(Entity entity, DealerAppearance appearance) throws ReflectiveOperationException {
      Object skinData = call(entity, "getEasyNPCSkinData");
      Class<?> entry = Class.forName("de.markusbordihn.easynpc.data.skin.SkinDataEntry");
      Object skin = entry.getConstructor(CompoundTag.class).newInstance(appearance.skin().copy());
      call(skinData, "setSkinDataEntry", new Class[]{entry}, skin);
      call(call(entity, "getEasyNPCVariantData"), "setSkinVariantType", new Class[]{String.class}, appearance.variant());
   }
}
