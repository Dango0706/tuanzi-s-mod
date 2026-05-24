package me.tuanzi.client.renderer;

import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import java.util.ArrayList;
import java.util.List;

public class TrialDummyRenderState extends ArmorStandRenderState {
    public final List<me.tuanzi.entity.DamageText> damageTexts = new ArrayList<>();
}
