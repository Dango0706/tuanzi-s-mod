package me.tuanzi.world.inventory.scavenger;

import net.minecraft.world.item.ItemStack;

/**
 * 公共端与客户端拾荒装备缓存状态之间的最小只读桥接层。
 * 由客户端入口在初始化阶段注册实现，避免公共菜单/槽位类在 Dedicated Server 加载时触发纯客户端类。
 */
public final class ScavengerClientBridge {

    public interface ClientStateProvider {
        ItemStack getEquippedBackpack();

        ItemStack getEquippedSafebox();

        boolean isBackpackEmpty();
    }

    private static final ClientStateProvider DEFAULT_PROVIDER = new ClientStateProvider() {
        @Override
        public ItemStack getEquippedBackpack() {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack getEquippedSafebox() {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isBackpackEmpty() {
            return false;
        }
    };

    private static volatile ClientStateProvider provider = DEFAULT_PROVIDER;

    private ScavengerClientBridge() {
    }

    public static void registerClientProvider(ClientStateProvider clientProvider) {
        provider = clientProvider != null ? clientProvider : DEFAULT_PROVIDER;
    }

    public static ItemStack getEquippedBackpack() {
        return provider.getEquippedBackpack();
    }

    public static ItemStack getEquippedSafebox() {
        return provider.getEquippedSafebox();
    }

    public static boolean isBackpackEmpty() {
        return provider.isBackpackEmpty();
    }
}
