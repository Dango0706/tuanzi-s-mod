package fr.minevegas.casino;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

public record CasinoPlayerDisplay(String id, String name) {
   public static final CasinoPlayerDisplay EMPTY = new CasinoPlayerDisplay("", "");
   public static final Codec<CasinoPlayerDisplay> CODEC = RecordCodecBuilder.<CasinoPlayerDisplay>create((instance) -> instance.group(Codec.STRING.optionalFieldOf("id", "").forGetter(CasinoPlayerDisplay::id), Codec.STRING.optionalFieldOf("name", "").forGetter(CasinoPlayerDisplay::name)).apply(instance, CasinoPlayerDisplay::new));

   public CasinoPlayerDisplay(String id, String name) {
      if (id == null || name == null) {
         id = "";
         name = "";
      }

      try {
         if (!id.isEmpty()) {
            UUID.fromString(id);
         }
      } catch (IllegalArgumentException var4) {
         id = "";
         name = "";
      }

      if (name.length() > 64) {
         name = name.substring(0, 64);
      }

      this.id = id;
      this.name = name;
   }

   public static CasinoPlayerDisplay of(Player player) {
      return player == null ? EMPTY : new CasinoPlayerDisplay(player.getUUID().toString(), player.getName().getString());
   }
}
