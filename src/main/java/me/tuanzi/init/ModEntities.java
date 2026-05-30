package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.entity.DecoyEntity;
import me.tuanzi.entity.ScarecrowEntity;
import me.tuanzi.entity.SeatEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;

public class ModEntities {
    public static final ResourceKey<EntityType<?>> DECOY_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "decoy")
    );

    public static final EntityType<DecoyEntity> DECOY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DECOY_KEY,
            EntityType.Builder.of(DecoyEntity::create, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .noLootTable()
                    .build(DECOY_KEY)
    );

    public static final ResourceKey<EntityType<?>> SCARECROW_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scarecrow")
    );

    public static final EntityType<ScarecrowEntity> SCARECROW = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            SCARECROW_KEY,
            EntityType.Builder.of(ScarecrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 1.975F)
                    .clientTrackingRange(10)
                    .noLootTable()
                    .build(SCARECROW_KEY)
    );

    public static final ResourceKey<EntityType<?>> TRIAL_DUMMY_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "trial_dummy")
    );

    public static final EntityType<me.tuanzi.entity.TrialDummyEntity> TRIAL_DUMMY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            TRIAL_DUMMY_KEY,
            EntityType.Builder.of(me.tuanzi.entity.TrialDummyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .noLootTable()
                    .build(TRIAL_DUMMY_KEY)
    );

    public static final ResourceKey<EntityType<?>> SEAT_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "seat")
    );

    public static final EntityType<SeatEntity> SEAT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            SEAT_KEY,
            EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(10)
                    .noLootTable()
                    .build(SEAT_KEY)
    );

    public static final ResourceKey<EntityType<?>> SHURIKEN_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "shuriken")
    );

    public static final EntityType<me.tuanzi.entity.ShurikenEntity> SHURIKEN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            SHURIKEN_KEY,
            EntityType.Builder.<me.tuanzi.entity.ShurikenEntity>of(me.tuanzi.entity.ShurikenEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .noLootTable()
                    .build(SHURIKEN_KEY)
    );

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(DECOY, DecoyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCARECROW, LivingEntity.createLivingAttributes());
        FabricDefaultAttributeRegistry.register(TRIAL_DUMMY, me.tuanzi.entity.TrialDummyEntity.createAttributes());
    }
}
