package com.zhiwei.skywar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * 渲染宿主。
 *
 * v1.5 起从 SurfaceView 改成普通 View。
 *
 * SurfaceView 走的是独立的一条 Surface：surfaceCreated → 渲染线程 lockCanvas →
 * unlockCanvasAndPost。这条链上任何一环出问题（buffer 协商失败、lockHardwareCanvas
 * 交出全黑帧、Surface 尺寸为 0、Surface 还没被 SurfaceFlinger 合成），结果都是
 * 「进程活着、声音照放、屏幕全黑」，而且不抛异常，代码里检测不到、也画不出提示。
 *
 * 普通 View 的 onDraw 由 ViewRootImpl 逐帧驱动，跟 Activity 窗口走同一条合成路径，
 * 不会出现「View 在、但画面不来」这种状态；绘制异常也还能在屏幕上写字。
 */
public class GameView extends View {

    public float W = 720f;
    public float H = 1280f;

    private float scale = 1f;
    private float offX = 0f;
    private float offY = 0f;

    private Scene scene;
    private final Paint fadePaint = new Paint();
    private final Paint diagPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Save save;
    public float time;
    public String versionName = "1.0";

    private boolean loopOn;
    private boolean artReady;
    private boolean buildPosted;
    private long lastNanos;
    private int frameCount;
    private String diag;

    private Runnable onFirstFrame;
    private boolean firstFrameFired;

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setKeepScreenOn(true);
        setBackgroundColor(Color.BLACK);

        fadePaint.setColor(Color.BLACK);

        diagPaint.setColor(0xFFFF5555);
        diagPaint.setTextSize(26f);
        diagPaint.setTypeface(Typeface.MONOSPACE);

        tipPaint.setColor(0xFF8FA6BC);
        tipPaint.setTextSize(28f);
        tipPaint.setTextAlign(Paint.Align.CENTER);
        tipPaint.setTypeface(Typeface.MONOSPACE);

        save = new Save(context);
        try {
            versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Throwable ignored) {
        }
        try {
            Sfx.init(context);
        } catch (Throwable ignored) {
        }
        applyPrefs();
    }

    public void applyPrefs() {
        try {
            Sfx.setEnabled(save.sfxOn == 1, save.musicOn == 1);
        } catch (Throwable ignored) {
        }
    }

    public void setScene(Scene s) {
        if (scene != null) {
            try {
                scene.onExit();
            } catch (Throwable ignored) {
            }
        }
        scene = s;
        if (scene != null) {
            try {
                scene.attach(this);
                scene.onEnter();
            } catch (Throwable t) {
                diag = "场景进入失败: " + t.getClass().getSimpleName();
            }
        }
        if (save.musicOn == 1) {
            try {
                Sfx.resumeMusic();
            } catch (Throwable ignored) {
            }
        }
        invalidate();
    }

    public Scene getScene() {
        return scene;
    }

    public void onResumeGame() {
        applyPrefs();
        startLoop();
        invalidate();
    }

    public void onPauseGame() {
        try {
            Sfx.pauseMusic();
        } catch (Throwable ignored) {
        }
        stopLoop();
    }

    public boolean handleBack() {
        return scene != null && scene.onBack();
    }

    /** 首帧绘制完成后回调一次，用于隐藏启动提示 */
    public void setOnFirstFrame(Runnable r) {
        onFirstFrame = r;
    }

    /** 已经画过多少帧。0 表示 View 一次绘制都没进过，说明布局/attach 层有问题 */
    public int getFrameCount() {
        return frameCount;
    }

    public String getDiag() {
        return diag;
    }

    // ---------------------------------------------------------------- 生命周期

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        computeGeometry(w, h);
        if (scene != null) {
            try {
                scene.onResize();
            } catch (Throwable ignored) {
            }
        }
        startLoop();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startLoop();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            startLoop();
            invalidate();
        } else {
            stopLoop();
        }
    }

    // ---------------------------------------------------------------- 几何

    private void computeGeometry(int w, int h) {
        if (w <= 0 || h <= 0) return;
        float s = w / 720f;
        float vh = h / s;
        if (vh < 1040f) {                 // 极扁屏幕，按高度保底
            s = h / 1040f;
            vh = 1040f;
        }
        if (!(s > 0.01f) || Float.isNaN(s) || Float.isInfinite(s)) return;
        scale = s;
        W = w / s;
        H = vh;
        if (Float.isNaN(W) || Float.isInfinite(W) || W < 200f) W = 720f;
        if (Float.isNaN(H) || Float.isInfinite(H) || H < 400f) H = 1280f;
        offX = 0f;
        offY = 0f;
    }

    // ---------------------------------------------------------------- 循环

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!loopOn) return;
            step();
            postOnAnimation(this);
        }
    };

    private void step() {
        long now = System.nanoTime();
        if (lastNanos == 0L) lastNanos = now;
        float dt = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        if (Float.isNaN(dt) || dt <= 0f) dt = 0.016f;
        if (dt > 0.06f) dt = 0.06f;
        if (dt < 0.0005f) dt = 0.0005f;
        time += dt;

        Scene sc = scene;
        if (sc != null) {
            try {
                sc.update(dt);
            } catch (Throwable t) {
                diag = "更新异常: " + t.getClass().getSimpleName();
            }
        }
        invalidate();
    }

    private void startLoop() {
        if (loopOn) return;
        if (getWidth() <= 0 || getHeight() <= 0) return;     // 等 onSizeChanged
        loopOn = true;
        lastNanos = 0L;
        postOnAnimation(tick);
    }

    private void stopLoop() {
        loopOn = false;
        removeCallbacks(tick);
    }

    /**
     * 美术烘焙放在第一帧之后做：先把「初始化资源中」这一帧画出去，
     * 再执行（可能几百毫秒的）位图烘焙，这样启动过程屏幕上一直有东西。
     */
    private final Runnable buildTask = new Runnable() {
        @Override
        public void run() {
            try {
                Art.build();
            } catch (Throwable t) {
                diag = "美术初始化失败: " + t.getClass().getSimpleName();
            }
            artReady = true;
            if (scene == null) {
                try {
                    setScene(new MenuScene());
                } catch (Throwable t) {
                    diag = "菜单初始化失败: " + t.getClass().getSimpleName();
                }
            }
            if (scene != null) {
                try {
                    scene.onResize();
                } catch (Throwable ignored) {
                }
            }
            invalidate();
        }
    };

    private boolean artOk() {
        return Art.player != null && Art.nebula != null && Art.bEnemy != null;
    }

    // ---------------------------------------------------------------- 绘制

    @Override
    protected void onDraw(Canvas c) {
        frameCount++;

        // 整屏铺底：View 的 Canvas 就是整个 View 区域，drawColor 一定盖满，
        // 不存在「绘制区比显示区小、边缘露出下层」的透明条。
        c.drawColor(0xFF000000);

        if (!artReady) {
            drawCentered(c, "初始化资源中…");
            if (!buildPosted) {
                buildPosted = true;
                post(buildTask);
            }
            fireFirstFrame();
            return;
        }

        if (!artOk()) {
            drawDiag(c, "美术资源未就绪\n" + (diag == null ? "无异常记录" : diag));
            fireFirstFrame();
            return;
        }

        Scene sc = scene;
        if (sc == null) {
            drawDiag(c, "场景未创建");
            fireFirstFrame();
            return;
        }

        try {
            // 复位共享画笔：上一帧残留的 shader / alpha / stroke 会让整屏发白变透
            Art.resetPaint();
            UI.resetPaints();

            c.save();
            c.translate(offX, offY);
            c.scale(scale, scale);
            sc.draw(c);
            if (sc.fade < 1f) {
                int a = (int) (255f * (1f - sc.fade));
                if (a < 0) a = 0;
                if (a > 255) a = 255;
                fadePaint.setColor(Color.BLACK);
                fadePaint.setAlpha(a);
                c.drawRect(0, 0, W, H, fadePaint);
            }
            c.restore();
        } catch (Throwable t) {
            try {
                c.restore();
            } catch (Throwable ignored) {
            }
            c.drawColor(0xFF000000);
            String m = t.getMessage();
            diag = "绘制异常: " + t.getClass().getSimpleName()
                    + (m == null ? "" : (": " + m));
        }

        if (diag != null) drawDiag(c, diag);
        fireFirstFrame();
    }

    private void drawCentered(Canvas c, String msg) {
        c.drawText(msg, getWidth() * 0.5f, getHeight() * 0.5f, tipPaint);
    }

    /** 诊断文字直接画在屏幕上：黑屏时必须能看见原因，不能只留纯黑 */
    private void drawDiag(Canvas c, String msg) {
        float y = getHeight() * 0.42f;
        String[] lines = msg.split("\n");
        for (String ln : lines) {
            if (ln == null || ln.length() == 0) continue;
            if (ln.length() > 40) ln = ln.substring(0, 40);
            c.drawText(ln, 20f, y, diagPaint);
            y += 34f;
        }
        c.drawText("v" + versionName + "  " + getWidth() + "x" + getHeight(),
                20f, y, diagPaint);
    }

    private void fireFirstFrame() {
        if (firstFrameFired) return;
        firstFrameFired = true;
        Runnable r = onFirstFrame;
        if (r != null) {
            try {
                r.run();
            } catch (Throwable ignored) {
            }
        }
    }

    // ---------------------------------------------------------------- 触摸

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
