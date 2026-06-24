package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class TranslucentVertexConsumer implements VertexConsumer {
    private final VertexConsumer parent;
    private final float alphaMultiplier;

    public TranslucentVertexConsumer(VertexConsumer parent, float alphaMultiplier) {
        this.parent = parent;
        this.alphaMultiplier = alphaMultiplier;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.parent.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        int newA = (int) (a * this.alphaMultiplier);
        this.parent.setColor(r, g, b, newA);
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        int a = (color >>> 24) & 0xFF;
        int newA = (int) (a * this.alphaMultiplier);
        int newColor = (color & 0x00FFFFFF) | (newA << 24);
        this.parent.setColor(newColor);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.parent.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.parent.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.parent.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.parent.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        this.parent.setLineWidth(width);
        return this;
    }
}
