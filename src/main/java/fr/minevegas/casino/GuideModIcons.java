package fr.minevegas.casino;

import com.mojang.blaze3d.platform.NativeImage;
import fr.minevegas.casino.platform.Services;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class GuideModIcons {
   private static final Map<String, Optional<Identifier>> ICONS = new HashMap();

   public static Identifier icon(String mod) {
      return (Identifier)((Optional)ICONS.computeIfAbsent(mod, GuideModIcons::load)).orElse((Object)null);
   }

   private static Optional<Identifier> load(String mod) {
      String legacyMca = Services.PLATFORM.loaderName().equals("Fabric") ? "fabric.net.mca.entity.VillagerLike" : "forge.net.mca.entity.VillagerLike";
      String[] anchors = mod.equals("easy_npc") ? new String[]{"de.markusbordihn.easynpc.entity.easynpc.EasyNPC"} : new String[]{"net.conczin.mca.entity.VillagerLike", legacyMca, "net.mca.entity.VillagerLike"};

      for(String name : anchors) {
         try {
            Class<?> anchor = Class.forName(name, false, GuideModIcons.class.getClassLoader());
            Path location = Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
            String[] paths = mod.equals("easy_npc") ? new String[]{"logo.png", "icon.png"} : new String[]{"mca.png", "assets/mca/icon.png", "icon.png", "logo.png"};
            if (Files.isDirectory(location, new LinkOption[0])) {
               for(String file : paths) {
                  if (Files.isRegularFile(location.resolve(file), new LinkOption[0])) {
                     InputStream stream = Files.newInputStream(location.resolve(file));

                     Optional var30;
                     try {
                        var30 = Optional.of(register(mod, NativeImage.read(stream)));
                     } catch (Throwable var22) {
                        if (stream != null) {
                           try {
                              stream.close();
                           } catch (Throwable var20) {
                              var22.addSuppressed(var20);
                           }
                        }

                        throw var22;
                     }

                     if (stream != null) {
                        stream.close();
                     }

                     return var30;
                  }
               }
            } else {
               JarFile jar = new JarFile(location.toFile());

               Optional var16;
               label117: {
                  try {
                     for(String file : paths) {
                        if (jar.getJarEntry(file) != null) {
                           InputStream stream = jar.getInputStream(jar.getJarEntry(file));

                           try {
                              var16 = Optional.of(register(mod, NativeImage.read(stream)));
                           } catch (Throwable var21) {
                              if (stream != null) {
                                 try {
                                    stream.close();
                                 } catch (Throwable var19) {
                                    var21.addSuppressed(var19);
                                 }
                              }

                              throw var21;
                           }

                           if (stream != null) {
                              stream.close();
                           }
                           break label117;
                        }
                     }
                  } catch (Throwable var23) {
                     try {
                        jar.close();
                     } catch (Throwable var18) {
                        var23.addSuppressed(var18);
                     }

                     throw var23;
                  }

                  jar.close();
                  continue;
               }

               jar.close();
               return var16;
            }
         } catch (LinkageError | Exception var24) {
         }
      }

      return Optional.empty();
   }

   private static Identifier register(String mod, NativeImage image) {
      Identifier id = RouletteMod.id("guide_compat/" + mod);
      Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "MineVegas guide: " + mod, image));
      return id;
   }

   private GuideModIcons() {
   }
}
