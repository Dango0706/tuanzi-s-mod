package me.tuanzi.client.renderer.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class SoulMerchantStationRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    public boolean hasVillager = false;
    public float spin = 0.0f;
    public net.minecraft.client.renderer.entity.state.EntityRenderState villagerRenderState;
}
