package fr.minevegas.casino;

public final class GuideOrbit {
   private static final float SPEED = 0.6F;
   private static final int RESUME_DELAY = 10;
   private float automatic;
   private float horizontal;
   private float vertical;
   private float zoom = 1.0F;
   private int age;
   private int resumeAt;
   private boolean held;

   public void tick() {
      ++this.age;
      if (this.advancing()) {
         this.automatic = (this.automatic + 0.6F) % 360.0F;
      }

   }

   public void press() {
      this.held = true;
   }

   public void release() {
      this.held = false;
      this.resumeAt = this.age + 10;
   }

   private boolean advancing() {
      return !this.held && this.age >= this.resumeAt;
   }

   public void drag(float dx, float dy) {
      this.horizontal = (this.horizontal + dx) % 360.0F;
      this.vertical = Math.max(-75.0F, Math.min(75.0F, this.vertical + dy));
   }

   public void zoom(double wheel) {
      this.zoom = Math.max(0.65F, Math.min(1.8F, this.zoom * (float)Math.pow(1.12, wheel)));
      this.resumeAt = this.age + 10;
   }

   public float yaw(float base, boolean spin, float partial) {
      return base + this.horizontal + (spin ? this.automatic + (this.advancing() ? partial * 0.6F : 0.0F) : 0.0F);
   }

   public float pitch(float base) {
      return Math.max(5.0F, Math.min(80.0F, base + this.vertical));
   }

   public float zoom() {
      return this.zoom;
   }
}
