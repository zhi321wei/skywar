package com.zhiwei.skywar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 首次启动的隐私政策同意界面（纯原生 View，不经过 Canvas，不影响游戏渲染路径）。
 *
 * 设计依据：
 * - 首次启动必须先展示隐私政策，用户点「同意」之后才能进入游戏；
 * - 「同意」与「不同意」两个按钮同等显著、均可点击，不使用「我知道了」之类的模糊文案；
 * - 同意之前不创建 GameView、不加载音效、不读写游戏存档，也就不存在任何数据触及。
 */
public final class PrivacyGate {

    private PrivacyGate() {
    }

    public static View build(final Activity act, final Runnable onAgree) {
        final int bg = 0xFF060A12;
        final int card = 0xFF0D1727;
        FrameLayout root = new FrameLayout(act);
        root.setBackgroundColor(bg);

        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(card);
        boxBg.setCornerRadius(dp(act, 16));
        boxBg.setStroke(dp(act, 1), 0xFF20364F);
        box.setBackground(boxBg);
        int pad = dp(act, 20);
        box.setPadding(pad, pad, pad, dp(act, 16));

        // 标题
        TextView title = new TextView(act);
        title.setText("隐私政策");
        title.setTextColor(0xFFDFF1FF);
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(title);

        // 副标题
        TextView ver = new TextView(act);
        ver.setText("苍穹战机 SkyWar　·　版本 v" + Privacy.POLICY_VERSION);
        ver.setTextColor(0xFF7E95AE);
        ver.setTextSize(12f);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vlp.topMargin = dp(act, 6);
        vlp.bottomMargin = dp(act, 12);
        box.addView(ver, vlp);

        // 正文（可滚动）
        ScrollView scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        TextView body = new TextView(act);
        body.setText(Privacy.text());
        body.setTextColor(0xFFC3D3E4);
        body.setTextSize(13f);
        body.setLineSpacing(dp(act, 2), 1.35f);
        body.setPadding(0, dp(act, 4), dp(act, 6), dp(act, 4));
        scroll.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(scroll, slp);

        // 查看线上完整版
        TextView link = new TextView(act);
        link.setText("在浏览器中查看完整版 →");
        link.setTextColor(0xFF4FD8FF);
        link.setTextSize(12.5f);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(act, 10);
        llp.bottomMargin = dp(act, 4);
        box.addView(link, llp);
        link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Privacy.POLICY_URL)));
                } catch (Throwable e) {
                    Toast.makeText(act, Privacy.POLICY_URL, Toast.LENGTH_LONG).show();
                }
            }
        });

        // 提示
        TextView hint = new TextView(act);
        hint.setText("请阅读以上条款后选择。点击「同意」表示您已阅读并同意本政策。");
        hint.setTextColor(0xFF7E95AE);
        hint.setTextSize(11.5f);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(act, 8);
        hlp.bottomMargin = dp(act, 12);
        box.addView(hint, hlp);

        // 按钮行：同等尺寸、同样式、均可点击
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int btnH = dp(act, 46);

        TextView deny = makeButton(act, "不同意", 0xFF243447, 0xFFA9BCCD);
        TextView agree = makeButton(act, "同意并继续", 0xFF123D52, 0xFF9FE4FF);

        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, btnH, 1f);
        p1.rightMargin = dp(act, 6);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, btnH, 1f);
        p2.leftMargin = dp(act, 6);
        row.addView(deny, p1);
        row.addView(agree, p2);
        box.addView(row);

        FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        int m = dp(act, 14);
        boxLp.setMargins(m, dp(act, 26), m, dp(act, 20));
        root.addView(box, boxLp);

        deny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(act)
                        .setTitle("无法继续")
                        .setMessage("不同意隐私政策将无法使用本游戏。\n"
                                + "本游戏不收集任何个人信息，也不申请任何权限，"
                                + "您随时可以在系统设置中清除本游戏的数据。")
                        .setPositiveButton("退出游戏", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                Privacy.setDeclined(act);
                                act.finish();
                            }
                        })
                        .setNegativeButton("再看看", null)
                        .show();
            }
        });

        agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Privacy.setAgreed(act);
                onAgree.run();
            }
        });

        return root;
    }

    private static TextView makeButton(Activity act, String text, int fill, int textColor) {
        TextView b = new TextView(act);
        b.setText(text);
        b.setTextSize(15f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(textColor);
        b.setGravity(Gravity.CENTER);
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(act, 10));
        d.setStroke(dp(act, 1), 0xFF2C4763);
        b.setBackground(d);
        return b;
    }

    private static int dp(Activity act, float v) {
        return Math.round(v * act.getResources().getDisplayMetrics().density);
    }
}
