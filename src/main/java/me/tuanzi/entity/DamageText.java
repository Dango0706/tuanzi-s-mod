package me.tuanzi.entity;

public class DamageText {
    public final float damage;
    public final long spawnTime;
    public double xOffset;
    public double yOffset;
    public double zOffset;
    public double xd;
    public double yd;
    public double zd;
    public boolean bounced = false;

    public DamageText(float damage, long spawnTime) {
        this.damage = damage;
        this.spawnTime = spawnTime;
        
        // 初始产生在受击位置，无初始偏移
        this.xOffset = 0.0;
        this.yOffset = 0.0;
        this.zOffset = 0.0;
        
        // 随机一个水平夹角，使得数字朝 360 度任意随机角度弹跳飞出
        double angle = Math.random() * 2.0 * Math.PI;
        double speed = 0.04 + Math.random() * 0.04;
        
        // 计算 3D 空间三维速度分量
        this.xd = Math.cos(angle) * speed;
        this.yd = 0.09 + Math.random() * 0.03; // 轻快地上升斜喷
        this.zd = Math.sin(angle) * speed;
    }

    public boolean tick() {
        long age = System.currentTimeMillis() - this.spawnTime;
        if (age > 1100) {
            return true; // 1.1 秒后消失
        }

        // 3D 抛体位移累加
        this.xOffset += this.xd;
        this.yOffset += this.yd;
        this.zOffset += this.zd;

        // 重力与空气阻力力学公式
        this.yd -= 0.015;
        this.xd *= 0.94;
        this.yd *= 0.95;
        this.zd *= 0.94;

        // 模拟触地轻快物理反弹与防过界
        if (this.yOffset <= -1.75) {
            this.yOffset = -1.75;
            if (!this.bounced) {
                this.yd = Math.abs(this.yd) * 0.40; // 向上反弹
                this.xd *= 0.60; // 触地水平速度减速摩擦
                this.zd *= 0.60;
                this.bounced = true;
            } else {
                this.yd = 0.0;
                this.xd = 0.0;
                this.zd = 0.0; // 落地后完全静止
            }
        }
        return false;
    }
}
