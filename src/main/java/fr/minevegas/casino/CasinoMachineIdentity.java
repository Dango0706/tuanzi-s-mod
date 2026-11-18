package fr.minevegas.casino;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CasinoMachineIdentity(String name, int number) {
   public static final CasinoMachineIdentity EMPTY = new CasinoMachineIdentity("", 0);
   public static final Codec<CasinoMachineIdentity> CODEC = RecordCodecBuilder.<CasinoMachineIdentity>create((instance) -> instance.group(Codec.STRING.optionalFieldOf("name", "").forGetter(CasinoMachineIdentity::name), Codec.INT.optionalFieldOf("number", 0).forGetter(CasinoMachineIdentity::number)).apply(instance, CasinoMachineIdentity::new));

   public CasinoMachineIdentity(String name, int number) {
      if (name != null && name.length() <= 64 && number >= 0 && !name.codePoints().anyMatch((c) -> Character.isISOControl(c) || c == 167)) {
         name = name.strip();
         this.name = name;
         this.number = number;
      } else {
         throw new IllegalArgumentException("machine name must be at most 64 characters, without control or formatting codes");
      }
   }
}
