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

    public static final ResourceKey<EntityType<?>> GLASS_ITEM_FRAME_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "glass_item_frame")
    );

    public static final EntityType<me.tuanzi.entity.GlassItemFrameEntity> GLASS_ITEM_FRAME = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            GLASS_ITEM_FRAME_KEY,
            EntityType.Builder.<me.tuanzi.entity.GlassItemFrameEntity>of(me.tuanzi.entity.GlassItemFrameEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(0.5F, 0.5F)
                    .eyeHeight(0.0F)
                    .clientTrackingRange(10)
                    .noUpdateInterval()
                    .dontTrackDeltas()
                    .build(GLASS_ITEM_FRAME_KEY)
    );

    public static final ResourceKey<EntityType<?>> CRESCENT_AURA_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "crescent_aura")
    );

    public static final EntityType<me.tuanzi.entity.CrescentAuraEntity> CRESCENT_AURA = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CRESCENT_AURA_KEY,
            EntityType.Builder.<me.tuanzi.entity.CrescentAuraEntity>of(me.tuanzi.entity.CrescentAuraEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(2.5F, 0.8F)
                    .clientTrackingRange(8)
                    .build(CRESCENT_AURA_KEY)
    );

    public static final ResourceKey<EntityType<?>> CINDER_SHIELD_PART_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_shield_part")
    );

    public static final EntityType<me.tuanzi.entity.CinderShieldPartEntity> CINDER_SHIELD_PART = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CINDER_SHIELD_PART_KEY,
            EntityType.Builder.<me.tuanzi.entity.CinderShieldPartEntity>of(me.tuanzi.entity.CinderShieldPartEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.6F)
                    .fireImmune()
                    .noLootTable()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(CINDER_SHIELD_PART_KEY)
    );

    public static final ResourceKey<EntityType<?>> CINDER_BLAZE_ROD_PART_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_blaze_rod_part")
    );

    public static final EntityType<me.tuanzi.entity.CinderBlazeRodPartEntity> CINDER_BLAZE_ROD_PART = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CINDER_BLAZE_ROD_PART_KEY,
            EntityType.Builder.<me.tuanzi.entity.CinderBlazeRodPartEntity>of(me.tuanzi.entity.CinderBlazeRodPartEntity::new, MobCategory.MISC)
                    .sized(0.35F, 1.2F)
                    .fireImmune()
                    .noLootTable()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(CINDER_BLAZE_ROD_PART_KEY)
    );

    public static final ResourceKey<EntityType<?>> CINDER_FIREBALL_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_fireball")
    );

    public static final EntityType<me.tuanzi.entity.CinderFireballEntity> CINDER_FIREBALL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CINDER_FIREBALL_KEY,
            EntityType.Builder.<me.tuanzi.entity.CinderFireballEntity>of(me.tuanzi.entity.CinderFireballEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .fireImmune()
                    .noLootTable()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(CINDER_FIREBALL_KEY)
    );

    public static final ResourceKey<EntityType<?>> CINDER_METEOR_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_meteor")
    );

    public static final EntityType<me.tuanzi.entity.CinderMeteorEntity> CINDER_METEOR = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CINDER_METEOR_KEY,
            EntityType.Builder.<me.tuanzi.entity.CinderMeteorEntity>of(me.tuanzi.entity.CinderMeteorEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .fireImmune()
                    .noLootTable()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(CINDER_METEOR_KEY)
    );

    public static final ResourceKey<EntityType<?>> CINDER_SOVEREIGN_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_sovereign")
    );

    public static final EntityType<me.tuanzi.entity.CinderSovereignEntity> CINDER_SOVEREIGN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CINDER_SOVEREIGN_KEY,
            EntityType.Builder.<me.tuanzi.entity.CinderSovereignEntity>of(me.tuanzi.entity.CinderSovereignEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 2.0F)
                    .eyeHeight(1.0F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build(CINDER_SOVEREIGN_KEY)
    );

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(DECOY, DecoyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCARECROW, LivingEntity.createLivingAttributes());
        FabricDefaultAttributeRegistry.register(TRIAL_DUMMY, me.tuanzi.entity.TrialDummyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CINDER_SOVEREIGN, me.tuanzi.entity.CinderSovereignEntity.createAttributes());
    }
}

