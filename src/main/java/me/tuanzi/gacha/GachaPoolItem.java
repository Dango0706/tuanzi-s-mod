package me.tuanzi.gacha;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import com.mojang.serialization.JsonOps;

import java.util.ArrayList;
import java.util.List;

public class GachaPoolItem {
    private final String id;
    private int weight;
    private final List<String> onObtainCommands;
    private final ItemStack item;

    public GachaPoolItem(String id, int weight, List<String> onObtainCommands, ItemStack item) {
        this.id = id;
        this.weight = weight;
        this.onObtainCommands = onObtainCommands != null ? onObtainCommands : new ArrayList<>();
        this.item = item != null ? item : ItemStack.EMPTY;
    }

    public String getId() {
        return id;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public List<String> getOnObtainCommands() {
        return onObtainCommands;
    }

    public ItemStack getItem() {
        return item;
    }

    // ──────────────────────────────────────────────
    // 原生 1.21+ Component / Registry 兼容编解码
    // ──────────────────────────────────────────────

    public JsonObject toJson(HolderLookup.Provider registries) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("weight", weight);
        
        if (!onObtainCommands.isEmpty()) {
            JsonArray cmdArray = new JsonArray();
            for (String cmd : onObtainCommands) {
                cmdArray.add(cmd);
            }
            json.add("on_obtain_commands", cmdArray);
        }

        // 利用 OPTIONAL_CODEC 将带注册表的复杂 ItemStack 完美无损编码成 JSON 结构
        JsonElement itemJson = ItemStack.OPTIONAL_CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), item)
                .getOrThrow();
        json.add("item", itemJson);

        return json;
    }

    public static GachaPoolItem fromJson(JsonObject json, HolderLookup.Provider registries) {
        String id = json.get("id").getAsString();
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 10;
        
        List<String> onObtainCommands = new ArrayList<>();
        if (json.has("on_obtain_commands")) {
            JsonArray array = json.getAsJsonArray("on_obtain_commands");
            for (JsonElement el : array) {
                onObtainCommands.add(el.getAsString());
            }
        }

        // 利用 OPTIONAL_CODEC 原生解码
        ItemStack item = ItemStack.OPTIONAL_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), json.get("item"))
                .result()
                .orElse(ItemStack.EMPTY);

        return new GachaPoolItem(id, weight, onObtainCommands, item);
    }
}
