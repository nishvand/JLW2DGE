package nishvand.Entities;

import nishvand.Objects.Sprite;

import java.awt.*;

public abstract class BasicEntity extends Sprite {
    boolean ai = false;
    protected int ticker = 0;
    protected int targetX = 0, targetY = 0, maxOffset = 128;
    public BasicEntity(Image image) {
        super(image,0,0,false);
    }
    public BasicEntity(Image image, int offsetX, int offsetY) { super(image, offsetX, offsetY, true); }
    public BasicEntity(Image image, int offsetX, int offsetY, boolean ai) { super(image, offsetX, offsetY, true); construct(ai); }
    private void construct(boolean ai){
        this.ai = ai;
    }

    protected abstract void soundReaction();
    protected void move(int x, int y) {

    }
}