package com.zhiwei.skywar;

import android.graphics.Canvas;

/**
 * 场景基类。所有坐标均为「虚拟坐标」：宽度固定 720，高度随屏幕比例变化。
 */
public abstract class Scene {

    protected GameView game;
    protected float W;
    protected float H;
    protected float t;          // 场景存活时间（秒）
    protected float fade;       // 进入淡入 0..1

    public void attach(GameView g) {
        this.game = g;
    }

    public void onEnter() {
        W = game.W;
        H = game.H;
        t = 0f;
        fade = 0f;
    }

    public void onExit() {
    }

    public void onResize() {
        W = game.W;
        H = game.H;
    }

    public void update(float dt) {
        t += dt;
        if (fade < 1f) fade = Math.min(1f, fade + dt * 4.5f);
    }

    public abstract void draw(Canvas c);

    /** action: 0=DOWN 1=MOVE 2=UP 3=CANCEL，坐标为虚拟坐标 */
    public void onTouch(int action, float x, float y) {
    }

    public boolean onBack() {
        return false;
    }
}
