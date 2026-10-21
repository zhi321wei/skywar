package com.zhiwei.skywar;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class GameActivity extends Activity {

    private GameView gameView;
    private TextView bootText;
    private FrameLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 先挂上崩溃记录，任何未捕获异常都会写进 Boot，下次启动可以直接看到
        installCrashHandler();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        bootText = new TextView(this);
        bootText.setTextColor(0xFF9FBAD2);
        bootText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        bootText.setGravity(Gravity.CENTER);
        bootText.setPadding(48, 48, 48, 48);
        bootText.setText("正在启动引擎…");

        try {
            gameView = new GameView(this);
        } catch (Throwable t) {
            Boot.crash(this, "new GameView", t);
            bootText.setText("启动失败\n\n" + t.getClass().getSimpleName()
                    + ": " + t.getMessage()
                    + "\n\n请截图反馈，或卸载后重新安装。");
            root.addView(bootText, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            setContentView(root);
            return;
        }

        // 顺序很重要：SurfaceView 在底层，提示文字在它上面
        root.addView(gameView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(bootText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        String lastCrash = Boot.lastCrash(this);
        bootText.setText("正在启动引擎…" + (lastCrash == null ? "" : "\n\n上次异常：\n" + lastCrash));

        gameView.onFirstFrame = new Runnable() {
            @Override
            public void run() {
                bootText.setVisibility(View.GONE);
                Boot.clearCrash(GameActivity.this);
            }
        };

        // 5 秒还没出第一帧，说明图形初始化卡住了，把状态显示出来而不是让用户对着黑屏
        gameView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (gameView == null || gameView.frameCount() > 0) return;
                String err = gameView.bootError();
                bootText.setVisibility(View.VISIBLE);
                bootText.setText("图形初始化超时\n\n"
                        + (err != null ? err + "\n\n" : "渲染线程未能输出画面。\n\n")
                        + "版本 v" + gameView.versionName
                        + "\n请截图反馈。");
            }
        }, 5000);

        setContentView(root);
        hideSystemUi();
    }

    private void installCrashHandler() {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        final android.content.Context ctx = getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Boot.crash(ctx, "crash@" + t.getName(), e);
                if (prev != null) {
                    prev.uncaughtException(t, e);
                }
            }
        });
    }

    private void hideSystemUi() {
        final View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        decor.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decor.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hideSystemUi();
                        }
                    }, 1500);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (gameView != null) gameView.onResumeGame();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.onPauseGame();
    }

    @Override
    public void onBackPressed() {
        if (gameView == null || !gameView.handleBack()) {
            super.onBackPressed();
        }
    }
}
