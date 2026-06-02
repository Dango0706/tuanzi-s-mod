package me.tuanzi.gacha;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;

import java.util.ArrayList;
import java.util.List;

public class GachaPool {
    private final String poolId;
    private String poolName;

    // 五个品质等级分类
    private final List<GachaPoolItem> legendary = new ArrayList<>();
    private final List<GachaPoolItem> epic = new ArrayList<>();
    private final List<GachaPoolItem> rare = new ArrayList<>();
    private final List<GachaPoolItem> uncommon = new ArrayList<>();
    private final List<GachaPoolItem> common = new ArrayList<>();

    public GachaPool(String poolId, String poolName) {
        this.poolId = poolId;
        this.poolName = poolName;
    }

    public String getPoolId() {
        return poolId;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public List<GachaPoolItem> getLegendary() {
        return legendary;
    }

    public List<GachaPoolItem> getEpic() {
        return epic;
    }

    public List<GachaPoolItem> getRare() {
        return rare;
    }

    public List<GachaPoolItem> getUncommon() {
        return uncommon;
    }

    public List<GachaPoolItem> getCommon() {
        return common;
    }

    // 根据品质等级字符串返回对应列表
    public List<GachaPoolItem> getListByRarity(String rarity) {
        switch (rarity.toLowerCase()) {
            case "legendary": return legendary;
            case "epic": return epic;
            case "rare": return rare;
            case "uncommon": return uncommon;
            case "common": return common;
            default: return common;
        }
    }

    // ──────────────────────────────────────────────
    // 编解码
    // ──────────────────────────────────────────────

    public JsonObject toJson(HolderLookup.Provider registries) {
        JsonObject json = new JsonObject();
        json.addProperty("pool_id", poolId);
        json.addProperty("pool_name", poolName);

        JsonObject itemsObj = new JsonObject();
        itemsObj.add("legendary", listToJson(legendary, registries));
        itemsObj.add("epic", listToJson(epic, registries));
        itemsObj.add("rare", listToJson(rare, registries));
        itemsObj.add("uncommon", listToJson(uncommon, registries));
        itemsObj.add("common", listToJson(common, registries));
        
        json.add("items", itemsObj);
        return json;
    }

    public static GachaPool fromJson(JsonObject json, HolderLookup.Provider registries) {
        String poolId = json.get("pool_id").getAsString();
        String poolName = json.get("pool_name").getAsString();

        GachaPool pool = new GachaPool(poolId, poolName);
        if (json.has("items")) {
            JsonObject itemsObj = json.getAsJsonObject("items");
            
            fillList(pool.legendary, itemsObj.getAsJsonArray("legendary"), registries);
            fillList(pool.epic, itemsObj.getAsJsonArray("epic"), registries);
            fillList(pool.rare, itemsObj.getAsJsonArray("rare"), registries);
            fillList(pool.uncommon, itemsObj.getAsJsonArray("uncommon"), registries);
            fillList(pool.common, itemsObj.getAsJsonArray("common"), registries);
        }
        return pool;
    }

    private JsonArray listToJson(List<GachaPoolItem> list, HolderLookup.Provider registries) {
        JsonArray array = new JsonArray();
        for (GachaPoolItem item : list) {
            array.add(item.toJson(registries));
        }
        return array;
    }

    private static void fillList(List<GachaPoolItem> list, JsonArray array, HolderLookup.Provider registries) {
        list.clear();
        if (array != null) {
            for (JsonElement el : array) {
                if (el.isJsonObject()) {
                    list.add(GachaPoolItem.fromJson(el.getAsJsonObject(), registries));
                }
            }
        }
    }
}
