package fr.minevegas.casinosound.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisAlloc;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class NativeOggStream implements AudioStream {
   private ByteBuffer compressed;
   private long decoder;
   private final AudioFormat format;
   private final int samples;

   public NativeOggStream(byte[] bytes) throws IOException {
      this.compressed = MemoryUtil.memAlloc(bytes.length);
      this.compressed.put(bytes).flip();

      try {
         try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            this.decoder = STBVorbis.stb_vorbis_open_memory(this.compressed, (IntBuffer)error, (STBVorbisAlloc)null);
            if (this.decoder == 0L) {
               throw new IOException("Vorbis open failed: " + error.get(0));
            }

            STBVorbisInfo info = STBVorbis.stb_vorbis_get_info(this.decoder, STBVorbisInfo.malloc(stack));
            this.format = new AudioFormat((float)info.sample_rate(), 16, info.channels(), true, false);
            this.samples = STBVorbis.stb_vorbis_stream_length_in_samples(this.decoder);
         }

      } catch (RuntimeException | Error | IOException e) {
         this.close();
         throw e;
      }
   }

   public int totalSamples() {
      return this.samples;
   }

   public void seekMs(long offset) throws IOException {
      int sample = (int)Math.min((double)Math.max(0, this.samples - 1), (double)((float)Math.max(0L, offset) * this.format.getSampleRate()) / (double)1000.0F);
      if (this.decoder != 0L && !STBVorbis.stb_vorbis_seek(this.decoder, sample)) {
         throw new IOException("Vorbis seek failed at " + sample);
      }
   }

   public AudioFormat getFormat() {
      return this.format;
   }

   public ByteBuffer read(int bytes) {
      int frameSize = this.format.getFrameSize();
      ByteBuffer pcm = BufferUtils.createByteBuffer(Math.max(frameSize, bytes / frameSize * frameSize));
      if (this.decoder == 0L) {
         pcm.limit(0);
         return pcm;
      } else {
         int frames = STBVorbis.stb_vorbis_get_samples_short_interleaved(this.decoder, this.format.getChannels(), pcm.asShortBuffer());
         pcm.limit(frames * frameSize);
         return pcm;
      }
   }

   public void close() {
      if (this.decoder != 0L) {
         STBVorbis.stb_vorbis_close(this.decoder);
         this.decoder = 0L;
      }

      if (this.compressed != null) {
         MemoryUtil.memFree(this.compressed);
         this.compressed = null;
      }

   }
}
