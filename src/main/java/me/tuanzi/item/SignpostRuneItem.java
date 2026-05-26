package me.tuanzi.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

public class SignpostRuneItem extends Item {

    public SignpostRuneItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // 必须处于潜行状态才会绑定位置
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        
        // 记录当前位置和维度
        CompoundTag data = new CompoundTag();
        data.putBoolean("Bound", true);
        data.putString("Dimension", level.dimension().identifier().toString());
        data.putDouble("X", player.getX());
        data.putDouble("Y", player.getY());
        data.putDouble("Z", player.getZ());
        data.putFloat("Yaw", player.getYRot());
        data.putFloat("Pitch", player.getXRot());

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

        // 播放效果
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.4, 0.6, 0.4, 0.15);

        // 玩家提示
        player.sendSystemMessage(Component.translatable("message.tuanzis_mod.signpost_rune.bound"));

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.copyTag().getBooleanOr("Bound", false)) {
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.signpost_rune.tooltip.unbound"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.signpost_rune.tooltip.bind_hint"));
            return;
        }

        CompoundTag tag = customData.copyTag();
        String dimension = tag.getStringOr("Dimension", "unknown");
        double x = tag.getDoubleOr("X", 0.0);
        double y = tag.getDoubleOr("Y", 0.0);
        double z = tag.getDoubleOr("Z", 0.0);

        // 维度翻译名称化
        String dimNameKey = "dimension." + dimension.replace(":", ".");
        net.minecraft.network.chat.MutableComponent dimComponent = Component.translatable(dimNameKey);

        // 拼接成精美的 [维度] (X, Y, Z) 形式展现给玩家
        tooltipComponents.accept(Component.literal("§a[")
                .append(dimComponent)
                .append("] §r§b(")
                .append(String.format("%.1f", x))
                .append(", ")
                .append(String.format("%.1f", y))
                .append(", ")
                .append(String.format("%.1f", z))
                .append(")"));
    }

    public static boolean isRuneBound(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        return customData.copyTag().getBooleanOr("Bound", false);
    }

    public static String getRuneDimension(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return "minecraft:overworld";
        return customData.copyTag().getStringOr("Dimension", "minecraft:overworld");
    }

    public static double getRuneX(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getDoubleOr("X", 0.0);
    }

    public static double getRuneY(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getDoubleOr("Y", 0.0);
    }

    public static double getRuneZ(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getDoubleOr("Z", 0.0);
    }

    public static float getRuneYaw(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0f;
        return customData.copyTag().getFloatOr("Yaw", 0.0f);
    }

    public static float getRunePitch(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0f;
        return customData.copyTag().getFloatOr("Pitch", 0.0f);
    }
}
