package com.zhiwei.skywar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 渲染宿主。
 *
 * v1.3 的渲染策略调整：
 *  - 默认走 lockCanvas() 软件渲染（v1.0 已验证稳定），硬件画布仅作兜底。
 *    部分机型 GPU 驱动在 lockHardwareCanvas 路径上会交出全黑帧，
 *    表现就是「游戏打开一片黑」。
 *  - 任何一帧画不出来，屏幕上都会显示可读的错误文字，不再只留纯黑。
 *  - 启动各阶段写入 Boot 记录，闪退后下次打开能看到上次的异常。
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "SkyWar";

    public float W = 720f;
    public float H = 1280f;

    private float scale = 1f;
    private float offX = 0f;
    private float offY = 0f;

    private Thread thread;
    private volatile boolean running;
    private volatile boolean surfaceReady;

    private Scene scene;
    private int drawErrs;
    private volatile int frames;
    private volatile String bootError;      // 启动期致命错误，直接画在屏幕上
    private volatile String lastError;      // 最近一次绘制异常（附加角标）
    private boolean firstFrameNotified;

    private final Paint fadePaint = new Paint();
    private final Paint safePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Save save;
    public float time;
    public String versionName = "1.0";
    public Runnable onFirstFrame;

    public GameView(Context context) {
        super(context);
        // 不再调用 setFormat()：SurfaceView 默认就是 PixelFormat.OPAQUE，
        // 显式设置反而在个别机型上影响 buffer 协商。
        getHolder().addCallback(this);
        setBackgroundColor(Color.BLACK);
        setFocusable(true);
        setKeepScreenOn(true);
        fadePaint.setColor(Color.BLACK);
        safePaint.setAntiAlias(true);

        Boot.note(context, "init");
        save = new Save(context);
        try {
            versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Throwable ignored) {
        }
        try {
            Sfx.init(context);
        } catch (Throwable t) {
            Boot.crash(context, "Sfx.init", t);
        }
        try {
            Art.build();
            Boot.note(context, "art");
        } catch (Throwable t) {
            bootError = "美术资源初始化失败：" + t;
            Boot.crash(context, "Art.build", t);
            Log.e(TAG, "Art.build failed", t);
        }
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
        if (save != null && save.musicOn == 1) Sfx.resumeMusic();
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

    /** 已经成功上屏的帧数，0 表示从未画出过一帧 */
    public int frameCount() {
        return frames;
    }

    public String bootError() {
        return bootError;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        Boot.note(getContext(), "surface");
        computeGeometry(holder);
        if (bootError == null && scene == null) {
            try {
                setScene(new MenuScene());
                Boot.note(getContext(), "menu");
            } catch (Throwable t) {
                bootError = "主菜单创建失败：" + t;
                Boot.crash(getContext(), "new MenuScene", t);
                Log.e(TAG, "MenuScene failed", t);
            }
        }
        resumeLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        computeGeometry(holder);
        if (scene != null) {
            try {
                scene.onResize();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        pauseLoop();
    }

    private void computeGeometry(SurfaceHolder holder) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            try {
                w = holder.getSurfaceFrame().width();
                h = holder.getSurfaceFrame().height();
            } catch (Throwable ignored) {
            }
        }
        if (w <= 0 || h <= 0) return;

        float s = w / 720f;
        float vh = h / s;
        if (vh < 1040f) {                 // 极扁屏幕，按高度保底
            s = h / 1040f;
            vh = 1040f;
        }
        if (s <= 0.01f || Float.isNaN(s) || Float.isInfinite(s)) return;

        scale = s;
        W = w / s;
        H = vh;
        // 极端情况下的几何兜底，避免裁剪区变成空集导致整屏黑
        if (W < 200f || Float.isNaN(W)) W = 720f;
        if (H < 400f || Float.isNaN(H)) H = 1280f;
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

            if (scene == null && bootError == null) {
                try {
                    setScene(new MenuScene());
                    Boot.note(getContext(), "menu");
                } catch (Throwable t) {
                    bootError = "主菜单创建失败：" + t;
                    Boot.crash(getContext(), "new MenuScene", t);
                }
            }

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
                // 软件画布优先：兼容性最好，不会出现「驱动黑帧」
                c = holder.lockCanvas();
                if (c == null) c = holder.lockHardwareCanvas();
                if (c != null) {
                    // 先复位共享画笔，再整屏铺不透明底色，杜绝残影/透明
                    Art.resetPaint();
                    UI.resetPaints();
                    c.drawColor(0xFF000000);

                    c.save();
                    c.translate(offX, offY);
                    c.scale(scale, scale);
                    c.clipRect(0, 0, W, H);

                    if (bootError != null) {
                        drawFatal(c, bootError);
                    } else if (sc != null) {
                        try {
                            sc.draw(c);
                        } catch (Throwable e) {
                            countDrawError("draw", e);
                        }
                        if (sc.fade < 1f) {
                            fadePaint.setAlpha((int) (255 * (1f - sc.fade)));
                            c.drawRect(0, 0, W, H, fadePaint);
                        }
                    } else {
                        drawFatal(c, "场景尚未就绪，请稍候…");
                    }

                    if (drawErrs > 0) drawErrorBadge(c);
                    c.restore();

                    frames++;
                    if (!firstFrameNotified) {
                        firstFrameNotified = true;
                        post(new Runnable() {
                            @Override
                            public void run() {
                                if (onFirstFrame != null) onFirstFrame.run();
                            }
                        });
                    }
                }
            } catch (Throwable t) {
                countDrawError("frame", t);
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

    private void countDrawError(String where, Throwable t) {
        drawErrs++;
        lastError = where + " · " + t.getClass().getSimpleName()
                + (t.getMessage() == null ? "" : ": " + t.getMessage());
        if (drawErrs <= 3 || drawErrs % 300 == 0) {
            Log.e(TAG, "render error #" + drawErrs + " (" + where + ")", t);
        }
        if (drawErrs == 3) {
            Boot.crash(getContext(), "render:" + where, t);
        }
    }

    /** 用最朴素的绘制把错误直接显示在画面上，绝不留下纯黑屏 */
    private void drawFatal(Canvas c, String msg) {
        safePaint.setStyle(Paint.Style.FILL);
        safePaint.setShader(null);
        safePaint.setAlpha(255);
        safePaint.setColor(0xFF08111E);
        c.drawRect(0, 0, W, H, safePaint);

        safePaint.setColor(0xFF4FE0FF);
        c.drawRect(40f, H * 0.30f, 44f, H * 0.30f + 6f, safePaint);

        float y = H * 0.30f + 60f;
        safePaint.setTextSize(26f);
        safePaint.setTextAlign(Paint.Align.LEFT);
        safePaint.setColor(0xFFE8F4FF);
        c.drawText("苍穹战机 · 启动遇到问题", 40f, y, safePaint);

        y += 46f;
        safePaint.setTextSize(17f);
        safePaint.setColor(0xFF8FA6BD);
        y = drawWrapped(c, msg == null ? "未知错误" : msg, 40f, y, W - 80f, 26f);

        y += 34f;
        safePaint.setColor(0xFF5B7186);
        safePaint.setTextSize(15f);
        String tip = "版本 v" + versionName + " · 请把这个画面截图反馈，或卸载重装一次";
        drawWrapped(c, tip, 40f, y, W - 80f, 24f);
    }

    /** 底部一行小字显示最近的渲染异常，便于定位 */
    private void drawErrorBadge(Canvas c) {
        safePaint.setStyle(Paint.Style.FILL);
        safePaint.setShader(null);
        safePaint.setAlpha(255);
        safePaint.setColor(0xCC2A0A0A);
        c.drawRect(0, H - 34f, W, H, safePaint);
        safePaint.setColor(0xFFFFB0A0);
        safePaint.setTextSize(12f);
        safePaint.setTextAlign(Paint.Align.LEFT);
        String s = "渲染告警 " + drawErrs + " 次 · " + (lastError == null ? "" : lastError);
        if (s.length() > 60) s = s.substring(0, 60) + "…";
        c.drawText(s, 10f, H - 12f, safePaint);
    }

    /** 中英混排的朴素折行（按字符宽度累计） */
    private float drawWrapped(Canvas c, String text, float x, float y, float maxW, float lh) {
        int start = 0;
        int len = text.length();
        float cy = y;
        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                c.drawText(text.substring(start, i), x, cy, safePaint);
                cy += lh;
                start = i + 1;
            } else if (i > start && safePaint.measureText(text.substring(start, i + 1)) > maxW) {
                c.drawText(text.substring(start, i), x, cy, safePaint);
                cy += lh;
                start = i;
            }
        }
        if (start < len) c.drawText(text.substring(start), x, cy, safePaint);
        return cy;
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
