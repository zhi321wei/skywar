package com.zhiwei.skywar;

/** 静态数据表：武器 / 超级武器 / 配件 / 关卡 / 数值计算 */
public class Data {

    public static final int STAGES = 10;
    public static final int MAX_LV = 6;

    // ---------------- 主武器 ----------------
    public static final String[] W_NAME = {"火神机炮", "脉冲激光", "离子散射"};
    public static final String[] W_TAG = {"高射速 / 直射", "穿透 / 单点高伤", "扇形 / 大范围"};
    public static final String[] W_DESC = {
            "三管旋转机炮，射速极快，弹道笔直。\n升级可增加并列炮管数量。",
            "压缩脉冲激光束，可贯穿敌机，\n对单个目标造成极高伤害。",
            "等离子散射弹幕，一次覆盖大范围，\n适合清理密集小型敌机。"
    };
    public static final int[] W_UNLOCK = {0, 1800, 2600};
    public static final int[] W_COST = {170, 250, 210};

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

    // ---------------- 消耗品 ----------------
    public static final String[] C_NAME = {"应急修理包", "护盾电池", "超武充能器"};
    public static final String[] C_DESC = {"开局装甲 +40", "开局护盾 +40", "开局超武能量 +50%"};
    public static final int[] C_PRICE = {320, 280, 420};

    // ---------------- 关卡 ----------------
    public static final String[] STAGE_NAME = {
            "星港警报", "陨石带伏击", "赤色风暴", "极地堡垒", "深空舰队",
            "幽蓝裂谷", "熔火要塞", "静默轨道", "破晓之城", "终焉之核"
    };

    public static String stageTitle(int stage) {
        int i = Math.max(0, Math.min(STAGES - 1, stage - 1));
        return STAGE_NAME[i];
    }

    public static int stageReward(int stage) {
        return 260 + 170 * stage;
    }

    public static int stageFirstBonus(int stage) {
        return 300 + 120 * stage;
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
        int base = W_COST[Math.max(0, Math.min(2, idx))];
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

    // ---------------- 战斗中属性 ----------------
    public static class Stats {
        public float maxHp;
        public float maxShield;
        public float speed;
        public float pickRadius;
        public float goldMul;
        public int sideGuns;
        public float fireInterval;
        public float damage;
        public float bulletSpeed;
        public int streams;
        public int pierce;
        public int spread;      // 散射弹数
        public int wType;
        public int sType;
        public int wLv;
        public int sLv;
    }

    public static Stats stats(Save s) {
        Stats st = new Stats();
        int lv = Math.max(1, s.weaponLv[s.weapon]);
        st.wType = s.weapon;
        st.wLv = lv;
        st.maxHp = 100 + 22 * s.partLv[0];
        st.speed = 560 + 42 * s.partLv[1];
        st.maxShield = 18 + 20 * s.partLv[2];
        st.pickRadius = 62 + 26 * s.partLv[3];
        st.goldMul = 1f + 0.06f * s.partLv[3];
        st.sideGuns = s.partLv[4];
        st.sType = s.sup;
        st.sLv = Math.max(1, s.supLv[s.sup]);
        st.bulletSpeed = 1180f;
        if (st.wType == 0) {
            st.fireInterval = 0.115f - 0.009f * (lv - 1);
            st.damage = 8f + 2.4f * (lv - 1);
            st.streams = 1 + (lv - 1) / 2;
            st.pierce = 0;
            st.spread = 0;
        } else if (st.wType == 1) {
            st.fireInterval = 0.22f - 0.014f * (lv - 1);
            st.damage = 22f + 6f * (lv - 1);
            st.streams = 1;
            st.pierce = 1 + lv / 2;
            st.spread = 0;
            st.bulletSpeed = 1500f;
        } else {
            st.fireInterval = 0.15f - 0.006f * (lv - 1);
            st.damage = 4.2f + 1.1f * (lv - 1);
            st.streams = 1;
            st.pierce = 0;
            st.spread = 2 + lv;
        }
        return st;
    }

    /** 单发/单束的估算 DPS，用于界面展示 */
    public static int dps(Stats st) {
        float shots = 1f / Math.max(0.05f, st.fireInterval);
        float perShot;
        if (st.wType == 2) perShot = st.damage * st.spread;
        else if (st.wType == 1) perShot = st.damage;
        else perShot = st.damage * st.streams * 1.35f;
        float side = st.sideGuns * st.damage * 0.55f / 0.24f;
        return (int) (shots * perShot + side);
    }
}
