package com.zhiwei.skywar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public float W = 720f;
    public float H = 1280f;

    private float scale = 1f;
    private float offX = 0f;
    private float offY = 0f;

    private Thread thread;
    private volatile boolean running;
    private boolean surfaceReady;

    private Scene scene;
    private final Paint blackPaint = new Paint();
    private final Paint fadePaint = new Paint();

    public Save save;
    public float time;
    public String versionName = "1.0";

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
        blackPaint.setColor(Color.BLACK);
        fadePaint.setColor(Color.BLACK);
        save = new Save(context);
        try {
            versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Throwable ignored) {
        }
        Sfx.init(context);
        Art.build();
        applyPrefs();
    }

    public void applyPrefs() {
        Sfx.setEnabled(save.sfxOn == 1, save.musicOn == 1);
    }

    public void setScene(Scene s) {
        if (scene != null) scene.onExit();
        scene = s;
        if (scene != null) {
            scene.attach(this);
            scene.onEnter();
        }
        if (save.musicOn == 1) Sfx.resumeMusic();
    }

    public Scene getScene() {
        return scene;
    }

    public void onResumeGame() {
        Sfx.setEnabled(save.sfxOn == 1, save.musicOn == 1);
        if (running && scene == null) setScene(new MenuScene());
    }

    public void onPauseGame() {
        Sfx.pauseMusic();
    }

    public boolean handleBack() {
        return scene != null && scene.onBack();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        computeGeometry(holder);
        if (scene == null) setScene(new MenuScene());
        resumeLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        computeGeometry(holder);
        if (scene != null) scene.onResize();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        pauseLoop();
    }

    private void computeGeometry(SurfaceHolder holder) {
        int w = getWidth() > 0 ? getWidth() : holder.getSurfaceFrame().width();
        int h = getHeight() > 0 ? getHeight() : holder.getSurfaceFrame().height();
        if (w <= 0 || h <= 0) return;
        scale = w / 720f;
        float vh = h / scale;
        if (vh < 1040f) {                 // 极扁屏幕，按高度保底
            scale = h / 1040f;
            vh = 1040f;
        }
        W = w / scale;
        H = vh;
        offX = 0f;
        offY = 0f;
    }

    private void resumeLoop() {
        if (running) return;
        running = true;
        thread = new Thread(this, "skywar-loop");
        thread.start();
    }

    private void pauseLoop() {
        running = false;
        Thread tr = thread;
        thread = null;
        if (tr != null) {
            try {
                tr.join(800);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000_000f;
            last = now;
            if (dt > 0.06f) dt = 0.06f;
            if (dt < 0.0005f) dt = 0.0005f;
            time += dt;

            Scene sc = scene;
            if (sc != null) {
                try {
                    sc.update(dt);
                } catch (Throwable ignored) {
                }
            }

            Canvas c = null;
            SurfaceHolder holder = getHolder();
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    c.drawRect(0, 0, getWidth(), getHeight(), blackPaint);
                    c.save();
                    c.translate(offX, offY);
                    c.scale(scale, scale);
                    c.clipRect(0, 0, W, H);
                    if (sc != null) {
                        try {
                            sc.draw(c);
                        } catch (Throwable ignored) {
                        }
                        if (sc.fade < 1f) {
                            fadePaint.setAlpha((int) (255 * (1f - sc.fade)));
                            c.drawRect(0, 0, W, H, fadePaint);
                        }
                    }
                    c.restore();
                }
            } catch (Throwable ignored) {
            } finally {
                if (c != null) {
                    try {
                        holder.unlockCanvasAndPost(c);
                    } catch (Throwable ignored) {
                    }
                }
            }

            // 简易帧率控制
            long sleep = 16_000_000L - (System.nanoTime() - now);
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L));
                } catch (InterruptedException ignored) {
                }
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        Scene sc = scene;
        if (sc == null) return true;
        int idx = e.getActionIndex();
        if (idx < 0 || idx >= e.getPointerCount()) idx = 0;
        float x = e.getX(idx) / scale - offX;
        float y = e.getY(idx) / scale - offY;
        int action;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                action = 0;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                action = 4;      // 第二根手指按下（用于超武按钮）
                break;
            case MotionEvent.ACTION_POINTER_UP:
                action = 5;
                break;
            case MotionEvent.ACTION_UP:
                action = 2;
                break;
            case MotionEvent.ACTION_CANCEL:
                action = 3;
                break;
            default:
                action = 1;
                break;
        }
        try {
            sc.onTouch(action, x, y);
        } catch (Throwable ignored) {
        }
        return true;
    }
}
