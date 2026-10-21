package com.zhiwei.skywar;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class GameActivity extends Activity {

    private GameView gameView;
    private TextView tip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        gameView = new GameView(this);
        root.addView(gameView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        tip = new TextView(this);
        tip.setTextColor(0xFF8FA6BC);
        tip.setTextSize(15f);
        tip.setGravity(Gravity.CENTER);
        tip.setTypeface(Typeface.MONOSPACE);
        tip.setText("正在启动引擎…");
        FrameLayout.LayoutParams tipLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        tipLp.gravity = Gravity.BOTTOM;
        tipLp.bottomMargin = 110;
        root.addView(tip, tipLp);

        setContentView(root);
        hideSystemUi();

        gameView.setOnFirstFrame(new Runnable() {
            @Override
            public void run() {
                if (tip != null) tip.setVisibility(View.GONE);
            }
        });

        // 兜底：4 秒连一帧都没画过（说明 View 根本没进入绘制流程），
        // 把诊断信息顶到屏幕上。正常情况首帧回调已经把这层隐藏了。
        root.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (gameView == null || tip == null) return;
                if (gameView.getFrameCount() > 0) return;
                String d = gameView.getDiag();
                tip.setTextColor(0xFFFF6666);
                tip.setText("界面未开始绘制：4 秒内没有任何一帧\n"
                        + "版本 v" + gameView.versionName
                        + " · 视图 " + gameView.getWidth() + "x" + gameView.getHeight()
                        + "\n" + (d == null ? "无异常记录" : d)
                        + "\n请截图反馈");
                tip.setVisibility(View.VISIBLE);
            }
        }, 4000);
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
