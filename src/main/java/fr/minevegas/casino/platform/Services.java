package fr.minevegas.casino.platform;

import java.util.ServiceLoader;

public final class Services {
   public static final CasinoPlatform PLATFORM = (CasinoPlatform)load(CasinoPlatform.class);

   private Services() {
   }

   public static <T> T load(Class<T> service) {
      return (T)ServiceLoader.load(service, Services.class.getClassLoader()).findFirst().orElseThrow(() -> new IllegalStateException("No " + service.getName() + " in META-INF/services: this JAR has no mod loader part"));
   }
}
