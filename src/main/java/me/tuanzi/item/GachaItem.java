package me.tuanzi.item;

import me.tuanzi.gacha.GachaLogic;
import me.tuanzi.gacha.GachaPool;
import me.tuanzi.gacha.PoolManager;
import me.tuanzi.gacha.gui.GachaPreviewMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.ints.IntArrayList;


import java.util.List;

public class GachaItem extends Item {
    private final String poolId;
    private final boolean isTenFold;

    public GachaItem(Properties properties, String poolId, boolean isTenFold) {
        super(properties);
        this.poolId = poolId;
        this.isTenFold = isTenFold;
    }

    public String getPoolId() {
        return poolId;
    }

    public boolean isTenFold() {
        return isTenFold;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            // 1. 蹲下右键：打开可能出货的物品与实时保底概率展示只读 GUI
            if (player.isSecondaryUseActive()) {
                GachaPool pool = PoolManager.getPool(poolId);
                if (pool == null) {
                    player.sendSystemMessage(Component.literal("§c[!] 未找到指定的卡池: " + poolId));
                    return InteractionResult.SUCCESS;
                }
                
                player.openMenu(new SimpleMenuProvider(
                        (id, playerInv, p) -> new GachaPreviewMenu(id, playerInv, pool),
                        Component.literal("卡池预览 - " + pool.getPoolName())
                ));
            } 
            // 2. 常规右键：开启抽卡
            else {
                // 扣除抽卡道具
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                
                // 执行核心抽卡算法
                List<GachaLogic.RollResult> results = GachaLogic.performRoll(serverPlayer, poolId, isTenFold);
                if (results.isEmpty()) {
                    return InteractionResult.SUCCESS;
                }

                // 筛选此次抽卡中最高的稀有度，用以渲染烟花粒子色彩
                String maxRarity = "common";
                for (GachaLogic.RollResult res : results) {
                    String r = res.getRarity();
                    if (r.equals("legendary")) {
                        maxRarity = "legendary";
                        break;
                    } else if (r.equals("epic") && !maxRarity.equals("legendary")) {
                        maxRarity = "epic";
                    } else if (r.equals("rare") && !maxRarity.equals("legendary") && !maxRarity.equals("epic")) {
                        maxRarity = "rare";
                    } else if (r.equals("uncommon") && maxRarity.equals("common")) {
                        maxRarity = "uncommon";
                    }
                }

                // 在聊天栏打印抽卡报告
                player.sendSystemMessage(Component.literal("§6★ ==================================== ★"));
                player.sendSystemMessage(Component.literal("§e§l✦ 恭喜你获得了以下卡牌奖励 ✦"));
                for (GachaLogic.RollResult res : results) {
                    ItemStack prize = res.getPoolItem().getItem().copy();
                    String rarityName = getRarityChinese(res.getRarity());
                    player.sendSystemMessage(Component.literal("§f- " + rarityName + " " + prize.getHoverName().getString() + " §bx" + prize.getCount()));
                }
                player.sendSystemMessage(Component.literal("§6★ ==================================== ★"));

                // 烟花实体与多彩粒子齐射爆炸效果，并在半空中生成专属防火的掉落物实体
                ServerLevel serverLevel = (ServerLevel) level;
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();

                if (isTenFold) {
                    // 十连抽：以玩家为中心圆周生成 10 个烟花火箭升空！并在圆周位置生成专属防火掉落物
                    int total = results.size();
                    for (int i = 0; i < total; i++) {
                        double angle = i * (2 * Math.PI / total);
                        double rx = px + Math.cos(angle) * 2.0;
                        double rz = pz + Math.sin(angle) * 2.0;
                        double ry = py + 0.5;

                        // 获取对应的抽卡奖品
                        GachaLogic.RollResult res = results.get(i);
                        ItemStack prize = res.getPoolItem().getItem().copy();

                        // 动态生成对应品质爆炸色彩的烟花火箭
                        ItemStack customFwStack = createFireworkStack(res.getRarity());
                        
                        net.minecraft.world.entity.projectile.FireworkRocketEntity firework = new net.minecraft.world.entity.projectile.FireworkRocketEntity(
                                serverLevel,
                                player,
                                rx, ry, rz,
                                customFwStack
                        );
                        
                        // 将抽卡大奖绑定到烟花实体，在其空中真正炸开的瞬间生出专属发光的防火掉落物
                        ((me.tuanzi.util.IGachaFirework) firework).tuanzi$setGachaData(prize, player.getUUID(), res.getRarity());
                        
                        serverLevel.addFreshEntity(firework);
                    }
                } else {
                    // 单抽：在玩家身体前方直线发射 1 个烟花火箭！
                    Vec3 look = player.getLookAngle();
                    double fx = px + look.x * 1.5;
                    double fy = py + 1.5;
                    double fz = pz + look.z * 1.5;

                    if (!results.isEmpty()) {
                        GachaLogic.RollResult res = results.get(0);
                        ItemStack prize = res.getPoolItem().getItem().copy();

                        // 动态生成对应品质爆炸色彩的烟花火箭
                        ItemStack customFwStack = createFireworkStack(res.getRarity());
                        
                        net.minecraft.world.entity.projectile.FireworkRocketEntity firework = new net.minecraft.world.entity.projectile.FireworkRocketEntity(
                                serverLevel,
                                player,
                                fx, fy, fz,
                                customFwStack
                        );
                        
                        
                        // 将抽卡大奖绑定到烟花实体，在其空中真正炸开的瞬间生出专属发光的防火掉落物
                        ((me.tuanzi.util.IGachaFirework) firework).tuanzi$setGachaData(prize, player.getUUID(), res.getRarity());
                        
                        serverLevel.addFreshEntity(firework);
                    }
                }

                // 根据出货最高品质渲染专属色彩的漫天亮星粒子和音效
                SimpleParticleType particleType = ParticleTypes.FIREWORK;
                if (maxRarity.equals("legendary")) {
                    particleType = ParticleTypes.TOTEM_OF_UNDYING;
                    serverLevel.playSound(null, px, py, pz, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                } else if (maxRarity.equals("epic")) {
                    particleType = ParticleTypes.WITCH;
                    serverLevel.playSound(null, px, py, pz, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                } else {
                    serverLevel.playSound(null, px, py, pz, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.0f);
                }

                // 生成粒子风暴
                for (int k = 0; k < 6; k++) {
                    serverLevel.sendParticles(
                            particleType,
                            px, py + 3.0, pz,
                            25,
                            1.2, 0.8, 1.2,
                            0.12
                    );
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    private String getRarityChinese(String key) {
        return switch (key.toLowerCase()) {
            case "legendary" -> "§6[传说] ";
            case "epic" -> "§d[史诗] ";
            case "rare" -> "§b[稀有] ";
            case "uncommon" -> "§2[优秀] ";
            default -> "§f[普通] ";
        };
    }

    private ItemStack createFireworkStack(String rarity) {
        ItemStack fwStack = new ItemStack(Items.FIREWORK_ROCKET);
        
        // 1. 根据品质决定形状和颜色
        FireworkExplosion.Shape shape = FireworkExplosion.Shape.SMALL_BALL;
        int color = 0xFFFFFF; // 默认白色
        boolean hasTrail = false;
        boolean hasTwinkle = false;
        
        switch (rarity.toLowerCase()) {
            case "legendary" -> {
                shape = FireworkExplosion.Shape.STAR; // 传说用星形！
                color = 0xFFAA00; // 耀眼金黄色
                hasTrail = true;
                hasTwinkle = true;
            }
            case "epic" -> {
                shape = FireworkExplosion.Shape.BURST; // 史诗用爆裂形！
                color = 0xD333FF; // 高贵紫色
                hasTrail = true;
                hasTwinkle = true;
            }
            case "rare" -> {
                shape = FireworkExplosion.Shape.LARGE_BALL; // 稀有用大球形！
                color = 0x2572E6; // 亮蔚蓝色
                hasTwinkle = true;
            }
            case "uncommon" -> {
                shape = FireworkExplosion.Shape.SMALL_BALL;
                color = 0x3BCC33; // 翠绿色
            }
            default -> {
                shape = FireworkExplosion.Shape.SMALL_BALL;
                color = 0xF0F0F0; // 普通白色
            }
        }
        
        // 2. 构造爆炸数据 (使用绝对安全的 IntArrayList)
        IntArrayList colorList = new IntArrayList(new int[]{color});
        FireworkExplosion explosion = new FireworkExplosion(
                shape,
                colorList,
                colorList, // 渐变为同种颜色
                hasTrail,
                hasTwinkle
        );
        
        // 3. 设置到 Fireworks 组件
        Fireworks fireworks = new Fireworks(1, List.of(explosion));
        fwStack.set(DataComponents.FIREWORKS, fireworks);
        
        return fwStack;
    }
}
