package com.zhiwei.skywar;

/** 静态数据表：武器 / 超级武器 / 配件 / 关卡 / 数值计算 */
public class Data {

    public static final int STAGES = 10;
    public static final int MAX_LV = 6;

    // ---------------- 主武器 ----------------
    public static final int WN = 4;     // 主武器种类数
    public static final String[] W_NAME = {"火神机炮", "脉冲激光", "离子散射", "追猎导弹"};
    public static final String[] W_TAG = {"高射速 / 直射", "穿透 / 单点高伤", "扇形 / 大范围", "自动追踪 / 齐射"};
    public static final String[] W_GLYPH = {"炮", "激", "散", "弹"};
    public static final String[] W_DESC = {
            "三管旋转机炮，射速极快，弹道笔直。\n升级可增加并列炮管数量。",
            "压缩脉冲激光束，可贯穿敌机，\n对单个目标造成极高伤害。",
            "等离子散射弹幕，一次覆盖大范围，\n适合清理密集小型敌机。",
            "红外制导追猎导弹，自动锁定最近目标，\n命中后产生小范围爆炸伤害。"
    };
    public static final int[] W_UNLOCK = {0, 1800, 2600, 3600};
    public static final int[] W_COST = {170, 250, 210, 290};

    // ---------------- 双武器挂架 ----------------
    public static final int DUAL_PRICE = 4600;
    public static final String DUAL_NAME = "双联火控挂架";
    public static final String DUAL_DESC = "加装第二主武器槽，可同时携带两种主武器，并激活融合效果";

    // ---------------- 融合表 ----------------
    public static final String[][] FUSION_NAME = {
            {"超载同频", "贯穿弹幕", "霰射机炮", "火箭齐射"},
            {"贯穿弹幕", "双相谐振", "棱镜裂变", "相位猎手"},
            {"霰射机炮", "棱镜裂变", "弹幕饱和", "饱和打击"},
            {"火箭齐射", "相位猎手", "饱和打击", "双联猎杀"},
    };
    public static final String[][] FUSION_DESC = {
            {"射速 +25%，伤害 +10%", "机炮弹获得穿透，射速 +10%", "散射 +2 发，射速 +12%", "导弹 +1 发，伤害 +15%"},
            {"机炮弹获得穿透，射速 +10%", "伤害 +30%，穿透 +1", "激光命中后炸裂 3 发散射弹", "导弹 +1 发，穿透 +1"},
            {"散射 +2 发，射速 +12%", "激光命中后炸裂 3 发散射弹", "散射 +3 发，射速 +10%", "导弹 +2 发，伤害 +12%"},
            {"导弹 +1 发，伤害 +15%", "导弹 +1 发，穿透 +1", "导弹 +2 发，伤害 +12%", "导弹 +2 发，装填 -25%"},
    };

    /** 融合参数：只描述加成，真正计算在 stats() 里 */
    static class Fuse {
        float rate = 1f;      // 射速倍率
        float dmg = 1f;       // 伤害倍率
        int spreadAdd;
        int missileAdd;
        int pierceAdd;
        int vulcanPierce;
        boolean laserSplit;
    }

    private static Fuse fuseOf(int a, int b) {
        int lo = Math.min(a, b), hi = Math.max(a, b);
        Fuse f = new Fuse();
        if (lo == 0 && hi == 0) {
            f.rate = 1.25f;
            f.dmg = 1.10f;
        } else if (lo == 0 && hi == 1) {
            f.vulcanPierce = 1;
            f.rate = 1.10f;
        } else if (lo == 0 && hi == 2) {
            f.spreadAdd = 2;
            f.rate = 1.12f;
        } else if (lo == 0 && hi == 3) {
            f.missileAdd = 1;
            f.dmg = 1.15f;
        } else if (lo == 1 && hi == 1) {
            f.dmg = 1.30f;
            f.pierceAdd = 1;
        } else if (lo == 1 && hi == 2) {
            f.laserSplit = true;
        } else if (lo == 1 && hi == 3) {
            f.missileAdd = 1;
            f.pierceAdd = 1;
            f.dmg = 1.15f;
        } else if (lo == 2 && hi == 2) {
            f.spreadAdd = 3;
            f.rate = 1.10f;
        } else if (lo == 2 && hi == 3) {
            f.missileAdd = 2;
            f.dmg = 1.12f;
        } else {
            f.missileAdd = 2;
            f.rate = 1.25f;
        }
        return f;
    }

    public static String fusionName(int a, int b) {
        return FUSION_NAME[c(a)][c(b)];
    }

    public static String fusionDesc(int a, int b) {
        return FUSION_DESC[c(a)][c(b)];
    }

    private static int c(int v) {
        return Math.max(0, Math.min(WN - 1, v));
    }

    // ---------------- 超级武器 ----------------
    public static final String[] S_NAME = {"战术核弹", "电磁风暴", "僚机编队"};
    public static final String[] S_DESC = {
            "引爆后清空全屏敌弹，\n对所有敌机造成巨额伤害。",
            "释放持续电磁脉冲，\n大幅减速并持续灼烧全场敌机。",
            "召唤僚机编队协同作战，\n期间提供额外火力支援。"
    };
    public static final int[] S_UNLOCK = {0, 2200, 2800};
    public static final int[] S_COST = {200, 240, 230};

    // ---------------- 配件 ----------------
    public static final String[] P_NAME = {"复合装甲", "矢量引擎", "护盾发生器", "磁力线圈", "翼下副炮"};
    public static final String[] P_UNIT = {"装甲", "速度", "护盾", "磁吸", "副炮"};
    public static final String[] P_DESC = {
            "每级 +22 最大装甲",
            "每级 +42 移动速度",
            "每级 +20 护盾容量",
            "每级扩大拾取范围与金币收益",
            "每级增加一对翼下副炮"
    };
    public static final int[] P_COST = {160, 180, 200, 150, 230};

    // ---------------- 关卡 ----------------
    public static final String[] STAGE_NAME = {
            "星港警报", "陨石带伏击", "赤色风暴", "极地堡垒", "深空舰队",
            "幽蓝裂谷", "熔火要塞", "静默轨道", "破晓之城", "终焉之核"
    };

    public static String stageTitle(int stage) {
        int i = Math.max(0, Math.min(STAGES - 1, stage - 1));
        return STAGE_NAME[i];
    }

    /** 每局金币收益统一按 1/4 结算，取整到 5 */
    private static int quarter(int v) {
        return Math.max(10, Math.round(v / 4f / 5f) * 5);
    }

    public static int stageReward(int stage) {
        return quarter(260 + 170 * stage);
    }

    public static int stageFirstBonus(int stage) {
        return quarter(300 + 120 * stage);
    }

    public static float stageDuration(int stage) {
        return 30f + stage * 2f;
    }

    public static float enemyScale(int stage) {
        return 1f + 0.34f * (stage - 1);
    }

    public static float bossHp(int stage) {
        return 520f + 600f * stage;
    }

    // ---------------- 花费 ----------------
    public static int weaponUpCost(int idx, int lv) {
        int base = W_COST[Math.max(0, Math.min(WN - 1, idx))];
        return (int) (base * (0.85f + lv * 0.55f) / 10) * 10;
    }

    public static int superUpCost(int idx, int lv) {
        int base = S_COST[Math.max(0, Math.min(2, idx))];
        return (int) (base * (0.85f + lv * 0.55f) / 10) * 10;
    }

    public static int partUpCost(int idx, int lv) {
        int base = P_COST[Math.max(0, Math.min(4, idx))];
        return (int) (base * (0.8f + lv * 0.5f) / 10) * 10;
    }

    // ---------------- 武器基础参数 ----------------
    public static class Wpn {
        public int type, lv;
        public float interval, damage, speed;
        public int streams, pierce, spread, missiles;
        public boolean laserSplit;
        public int vulcanPierce;
    }

    /**
     * 主武器升级强度换算。
     * v1.8 起把 1~6 级的成长曲线整体压进旧的 1~4 级区间：
     * 1 级 = 旧 1 级，满级（6 级）= 旧 4 级。
     */
    public static float effLv(int lv) {
        int l = Math.max(1, Math.min(MAX_LV, lv));
        return 1f + (l - 1) * ((4f - 1f) / (MAX_LV - 1f));
    }

    public static Wpn wpn(int type, int lv) {
        Wpn w = new Wpn();
        w.type = c(type);
        w.lv = Math.max(1, Math.min(MAX_LV, lv));
        float e = effLv(w.lv);       // 满级只到旧 4 级
        float g = e - 1f;            // 成长量
        w.speed = 1180f;
        w.streams = 1;
        if (w.type == 0) {
            w.interval = 0.115f - 0.009f * g;
            w.damage = 8f + 2.4f * g;
            w.streams = 1 + (int) (g / 2f);
        } else if (w.type == 1) {
            w.interval = 0.22f - 0.014f * g;
            w.damage = 22f + 6f * g;
            w.pierce = 1 + (int) (e / 2f);
            w.speed = 1500f;
        } else if (w.type == 2) {
            w.interval = 0.15f - 0.006f * g;
            w.damage = 4.2f + 1.1f * g;
            w.spread = 2 + Math.round(e);
        } else {
            w.interval = 0.75f - 0.04f * g;
            w.damage = 18f + 6f * g;
            w.missiles = 1 + (int) (e / 2f);
            w.speed = 620f;
        }
        return w;
    }

    // ---------------- 战斗中属性 ----------------
    public static class Stats {
        public float maxHp;
        public float maxShield;
        public float speed;
        public float pickRadius;
        public float goldMul;
        public int sideGuns;

        // 主武器 A
        public Wpn a;
        // 副武器 B（未解锁为 null）
        public Wpn b;

        // 融合
        public boolean fused;
        public String fusionName;
        public String fusionDesc;

        public int sType;
        public int sLv;

        // 兼容旧字段（= 主武器 A 的展开）
        public int wType, wLv;
        public float fireInterval, damage, bulletSpeed;
        public int streams, pierce, spread;
    }

    public static Stats stats(Save s) {
        Stats st = new Stats();
        st.maxHp = 100 + 22 * s.partLv[0];
        st.speed = 560 + 42 * s.partLv[1];
        st.maxShield = 18 + 20 * s.partLv[2];
        st.pickRadius = 62 + 26 * s.partLv[3];
        st.goldMul = 1f + 0.06f * s.partLv[3];
        st.sideGuns = s.partLv[4];
        st.sType = s.sup;
        st.sLv = Math.max(1, s.supLv[s.sup]);

        int lvA = Math.max(1, s.weaponLv[c(s.weapon)]);
        Wpn a = wpn(s.weapon, lvA);
        Wpn b = null;
        if (s.dual > 0 && s.weapon2 >= 0 && s.weapon2 < WN && s.weaponLv[c(s.weapon2)] > 0) {
            b = wpn(s.weapon2, s.weaponLv[c(s.weapon2)]);
            // 副武器独立供弹，射速略低于主武器
            b.interval *= 1.18f;
        }

        if (b != null) {
            Fuse f = fuseOf(a.type, b.type);
            st.fused = true;
            st.fusionName = fusionName(a.type, b.type);
            st.fusionDesc = fusionDesc(a.type, b.type);
            a.interval /= f.rate;
            b.interval /= f.rate;
            a.damage *= f.dmg;
            b.damage *= f.dmg;
            if (a.type == 2) a.spread += f.spreadAdd;
            if (b.type == 2) b.spread += f.spreadAdd;
            if (a.type == 3) a.missiles += f.missileAdd;
            if (b.type == 3) b.missiles += f.missileAdd;
            if (a.type == 1) a.pierce += f.pierceAdd;
            if (b.type == 1) b.pierce += f.pierceAdd;
            if (a.type == 0) a.vulcanPierce = f.vulcanPierce;
            if (b.type == 0) b.vulcanPierce = f.vulcanPierce;
            a.laserSplit = f.laserSplit;
            b.laserSplit = f.laserSplit;
        }

        st.a = a;
        st.b = b;
        st.wType = a.type;
        st.wLv = a.lv;
        st.fireInterval = a.interval;
        st.damage = a.damage;
        st.bulletSpeed = a.speed;
        st.streams = a.streams;
        st.pierce = a.pierce;
        st.spread = a.spread;
        return st;
    }

    /** 估算 DPS（含副武器与融合） */
    public static int dps(Stats st) {
        float total = dpsOf(st.a, 1f);
        if (st.b != null) total += dpsOf(st.b, 0.92f);
        float side = st.sideGuns * st.a.damage * 0.55f / 0.24f;
        return (int) (total + side);
    }

    private static float dpsOf(Wpn w, float trust) {
        float shots = 1f / Math.max(0.05f, w.interval);
        float perShot;
        if (w.type == 2) perShot = w.damage * w.spread;
        else if (w.type == 3) perShot = w.damage * w.missiles * 1.15f;
        else if (w.type == 1) perShot = w.damage * (1f + w.pierce * 0.35f);
        else perShot = w.damage * w.streams * (1f + w.vulcanPierce * 0.4f) * 1.35f;
        return shots * perShot * trust;
    }

    /** 单把武器的简要数值描述，用于界面 */
    public static String wpnStat(Wpn w) {
        String s = "伤害 " + Math.round(w.damage) + " · 射速 " + String.format("%.1f", 1f / w.interval) + "/s";
        if (w.type == 0 && w.streams > 1) s += " · " + w.streams + " 束";
        if (w.type == 1) s += " · 穿透 " + w.pierce;
        if (w.type == 2) s += " · " + w.spread + " 发";
        if (w.type == 3) s += " · " + w.missiles + " 发";
        return s;
    }
}
