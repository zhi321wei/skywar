package com.zhiwei.skywar;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 隐私政策文本与「是否已同意」状态。
 *
 * 合规要点（TapTap 安全合规指南 / 工信部《App违法违规收集使用个人信息行为认定方法》）：
 * 1. App 首次启动必须弹窗告知隐私政策，且必须有「同意」和「拒绝」两个明确选项；
 * 2. 用户同意之前，不得初始化任何可能触及个人信息的组件（本作的 GameView / Sfx / 存档全部推迟到同意之后创建）；
 * 3. App 内展示的政策内容须与商店后台填写的隐私政策链接内容一致（本文本即线上页面的正文）。
 *
 * 线上完整版：https://zhi321wei.github.io/skywar/privacy.html
 */
public final class Privacy {

    /** 与线上页面保持一致的版本号 */
    public static final String POLICY_VERSION = "1.0";
    public static final String POLICY_DATE = "2026 年 10 月 26 日";
    public static final String POLICY_URL = "https://zhi321wei.github.io/skywar/privacy.html";

    private static final String SP = "skywar_privacy";
    private static final String KEY_AGREED = "agreed_ver";

    private Privacy() {
    }

    /** 是否已同意当前版本的政策；政策升版后会重新征求同意 */
    public static boolean agreed(Context c) {
        try {
            SharedPreferences sp = c.getSharedPreferences(SP, Context.MODE_PRIVATE);
            return POLICY_VERSION.equals(sp.getString(KEY_AGREED, null));
        } catch (Throwable e) {
            return false;
        }
    }

    /** 记录已同意 */
    public static void setAgreed(Context c) {
        try {
            c.getSharedPreferences(SP, Context.MODE_PRIVATE)
                    .edit().putString(KEY_AGREED, POLICY_VERSION).apply();
        } catch (Throwable ignored) {
        }
    }

    /** 用户选择「不同意」后不再反复弹窗打扰，但仍标记为未同意 */
    public static void setDeclined(Context c) {
        // 不作持久化：下次启动仍会再次征求同意，符合「不得默认同意」的要求
    }

    /**
     * 弹窗内展示的政策正文。与线上页面正文一致，为保证手机可读做了换行排版。
     */
    public static String text() {
        StringBuilder b = new StringBuilder(4096);
        b.append("《苍穹战机》（英文名 SkyWar，应用包名 com.zhiwei.skywar）由个人开发者独立开发与运营，")
                .append("为纯单机游戏。请您在使用前阅读以下内容。\n\n");

        b.append("一、我们收集哪些信息\n")
                .append("本游戏不收集任何个人信息。不需要注册或登录，没有账号体系；不采集设备标识符")
                .append("（IMEI、Android ID、OAID、MAC 地址等）；不采集位置、通讯录、短信、相册、")
                .append("麦克风、摄像头内容；不采集任何可用于识别个人身份的信息。\n\n");

        b.append("二、我们在设备上保存什么\n")
                .append("为让您下次打开时进度不丢失，本游戏会在您自己的设备本地保存一份存档，内容包括：")
                .append("金币数量、武器解锁与等级、配件强化等级、关卡解锁与最佳成绩、音量开关设置。")
                .append("这些数据存放于应用私有目录（Android SharedPreferences），仅存在于本机、")
                .append("仅用于本游戏读取，不会上传、不会共享给任何第三方。卸载本游戏即一并删除。")
                .append("本游戏不提供云存档，不存在服务器端副本。\n\n");

        b.append("三、系统权限说明\n")
                .append("本游戏的安装包未声明任何系统权限，包括网络访问、存储与相册读写、位置信息、")
                .append("通讯录与短信、麦克风与摄像头、设备信息与电话状态、悬浮窗与通知。")
                .append("由于未申请网络访问权限（android.permission.INTERNET），在系统层面即无法建立网络连接，")
                .append("因此不存在把数据上传到服务器的可能。您可在「设置 → 应用 → 苍穹战机 → 权限」中查看，该列表为空。\n\n");

        b.append("四、第三方服务与共享\n")
                .append("本游戏不接入任何第三方 SDK，包括但不限于广告平台、数据统计、推送服务、")
                .append("登录分享、支付与崩溃上报服务。由于不收集任何信息，因此不存在向第三方提供、")
                .append("共享、转让或公开披露个人信息的情形。游戏内所有音效与背景音乐均由开发者自行合成，")
                .append("所有美术资源均由程序实时绘制，不含需要联网加载或需要授权的外部素材。\n\n");

        b.append("五、与支付相关\n")
                .append("本游戏完全免费，不包含任何内购、订阅或付费内容。游戏内金币通过正常游玩获得，")
                .append("不支持用真实货币购买，也不存在任何形式的广告变现。\n\n");

        b.append("六、未成年人保护\n")
                .append("本游戏为太空射击题材的休闲游戏，不含不适宜未成年人的内容，不含社交与聊天功能，")
                .append("不含付费环节。由于不收集任何个人信息，也不存在收集未成年人个人信息的情形。\n\n");

        b.append("七、你的权利\n")
                .append("由于本游戏不在服务器端保存任何数据，您对自己的游戏数据拥有完全控制权：")
                .append("可在系统设置中清除本游戏的应用数据，或直接卸载本游戏，全部存档即被删除且不可恢复；")
                .append("全部游戏进度均可在游戏内的机库、商店、关卡界面直接查看。\n\n");

        b.append("八、政策更新\n")
                .append("如果将来本游戏新增联网功能（例如排行榜、云存档、广告），会在功能上线前更新本政策，")
                .append("并在游戏内或线上页面显著位置提示，同时重新征求您的同意。若您不同意更新后的政策，")
                .append("可以停止使用并卸载本游戏。\n\n");

        b.append("九、联系我们\n")
                .append("应用名称：苍穹战机（SkyWar）\n")
                .append("包名：com.zhiwei.skywar\n")
                .append("开发者：个人开发者\n")
                .append("联系邮箱：zhi321wei@users.noreply.github.com\n")
                .append("问题反馈：https://github.com/zhi321wei/skywar/issues\n")
                .append("我们会在收到反馈后 15 个工作日内答复。\n\n");

        b.append("完整版隐私政策：\n").append(POLICY_URL).append("\n\n");
        b.append("生效日期：").append(POLICY_DATE).append("　版本 v").append(POLICY_VERSION);

        return b.toString();
    }
}
