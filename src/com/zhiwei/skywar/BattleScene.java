package com.zhiwei.skywar;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.ArrayList;
import java.util.Random;

/** 战斗场景：纵向卷轴空战 */
public class BattleScene extends Scene {

    private static final int E_SCOUT = 0, E_ZIG = 1, E_TANK = 2, E_KAM = 3,
            E_SHIELD = 4, E_SPLIT = 5, E_SNIPE = 6, E_SWARM = 7, E_BOSS = 8;

    private static final int P_COIN = 0, P_EN = 1, P_SHIELD = 2, P_REPAIR = 3;

    private final Random rnd = new Random();

    private int stage;
    private Data.Stats st;

    private float px, py;
    private float hp, maxHp, shield, maxShield;
    private float energy, maxEnergy = 100f;
    private float fireT, fireT2, sideT, invuln;
    private float engineT;

    private int score;
    private int earned;
    private int kills;

    private float stageTime;
    private float spawnT;
    private boolean bossSpawned;
    private boolean cleared, failed, endSeq;
    private float endT;
    private float bossWarnT;

    private float bgScroll;
    private float shake;
    private float nukeFlash;
    private float empTime;
    private float wingTime;

    private final ArrayList<Bullet> pBullets = new ArrayList<Bullet>();
    private final ArrayList<Bullet> eBullets = new ArrayList<Bullet>();
    private final ArrayList<Enemy> enemies = new ArrayList<Enemy>();
    private final ArrayList<Particle> parts = new ArrayList<Particle>();
    private final ArrayList<Power> powers = new ArrayList<Power>();
    private final ArrayList<FText> texts = new ArrayList<FText>();

    private Enemy boss;
    private int idSeq;

    private boolean dragging;
    private float dragDX, dragDY;
    private float fingerX, fingerY;

    private boolean paused;
    private int pressed = -1;

    private final RectF bPause = new RectF();
    private final RectF bResume = new RectF();
    private final RectF bRestart = new RectF();
    private final RectF bQuit = new RectF();
    private float btnSuperX, btnSuperY, btnSuperR = 56f;

    private final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ================= 实体 =================
    static class Bullet {
        float x, y, vx, vy, dmg, r;
        int kind;          // 0 火神 1 激光 2 等离子 3 敌方小球 4 敌方大球 5 追踪导弹
        int pierce;
        float angle;
        float life;
        int lastHit = -1;
        int target = -1;   // 追踪目标 id
        float turn;        // 转向角速度 rad/s
        float accel;       // 加速度
        boolean aoe;       // 命中溅射
        boolean split;     // 激光裂变
    }

    static class Enemy {
        int type;
        float x, y, vx, vy, hp, maxHp, r;
        float t, baseX, amp, freq;
        float fireT;
        int pattern;
        float hitFlash;
        boolean entered;
        float capY;
        float aimX, aimY;
        int id;
        float guard;       // 护盾机剩余护盾
        float charge;      // 狙击机蓄力计时
    }

    static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        int color;
        float drag;
    }

    static class Power {
        int type;
        float x, y, vy, t;
    }

    static class FText {
        String s;
        float x, y, life;
        int color;
        float size;
    }

    public BattleScene(int stage) {
        this.stage = Math.max(1, Math.min(Data.STAGES, stage));
    }

    // ================= 生命周期 =================
    @Override
    public void onEnter() {
        super.onEnter();
        st = Data.stats(game.save);
        maxHp = st.maxHp;
        hp = maxHp;
        maxShield = st.maxShield;
        shield = maxShield;
        px = W / 2f;
        py = H - 180f;
        invuln = 1.2f;

        // 开局补给消耗
        if (game.save.cons[0] > 0) {
            game.save.cons[0]--;
            maxHp += 40f;
            hp = maxHp;
        }
        if (game.save.cons[1] > 0) {
            game.save.cons[1]--;
            maxShield += 40f;
            shield = maxShield;
        }
        if (game.save.cons[2] > 0) {
            game.save.cons[2]--;
            energy = maxEnergy * 0.5f;
        }
        game.save.save();

        bPause.set(W - 78f, 26f, W - 22f, 82f);
        btnSuperX = W - 92f;
        btnSuperY = H - 100f;
        float pw = 420f;
        float pl = (W - pw) / 2f;
        float pt = H * 0.5f - 190f;
        bResume.set(pl, pt + 110f, pl + pw, pt + 182f);
        bRestart.set(pl, pt + 198f, pl + pw, pt + 270f);
        bQuit.set(pl, pt + 286f, pl + pw, pt + 358f);
    }

    @Override
    public void onResize() {
        super.onResize();
        bPause.set(W - 78f, 26f, W - 22f, 82f);
        btnSuperX = W - 92f;
        btnSuperY = H - 100f;
    }

    // ================= 逻辑 =================
    @Override
    public void update(float dt) {
        t += dt;
        if (fade < 1f) fade = Math.min(1f, fade + dt * 4.5f);
        if (paused) return;

        if (nukeFlash > 0) nukeFlash = Math.max(0f, nukeFlash - dt * 2.2f);
        if (shake > 0) shake = Math.max(0f, shake - dt * 26f);
        if (empTime > 0) empTime = Math.max(0f, empTime - dt);
        if (wingTime > 0) wingTime = Math.max(0f, wingTime - dt);
        if (invuln > 0) invuln = Math.max(0f, invuln - dt);
        if (bossWarnT > 0) bossWarnT = Math.max(0f, bossWarnT - dt);

        bgScroll += dt * 160f;

        if (failed || cleared) {
            endT += dt;
            updateParticles(dt);
            updateBullets(dt);
            updatePowers(dt);
            if (endT > 1.5f) {
                goResult();
            }
            return;
        }

        stageTime += dt;
        updatePlayer(dt);
        updateSpawning(dt);
        updateEnemies(dt);
        updateBullets(dt);
        updateParticles(dt);
        updatePowers(dt);
        updateTexts(dt);
        updateWingmen(dt);
        if (empTime > 0) empTick(dt);
        checkCollisions();
    }

    private void goResult() {
        boolean first = false;
        int reward;
        if (cleared) {
            first = game.save.bestScore[stage - 1] == 0;
            reward = Data.stageReward(stage) + (first ? Data.stageFirstBonus(stage) : 0);
            if (score > game.save.bestScore[stage - 1]) game.save.bestScore[stage - 1] = score;
            if (stage + 1 > game.save.maxStage) game.save.maxStage = Math.min(Data.STAGES, stage + 1);
            game.save.clears++;
            game.save.totalKills += kills;
            game.save.earn(reward + earned);
            game.save.save();
        } else {
            reward = 0;
            game.save.totalKills += kills;
            game.save.earn(earned);
            game.save.save();
        }
        game.setScene(new ResultScene(stage, score, earned, reward, cleared, first, kills));
    }

    // ---------------- 玩家 ----------------
    private void updatePlayer(float dt) {
        if (dragging) {
            float tx = fingerX + dragDX;
            float ty = fingerY + dragDY;
            float maxStep = st.speed * 1.55f * dt;
            float dx = tx - px, dy = ty - py;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > maxStep) {
                dx = dx / d * maxStep;
                dy = dy / d * maxStep;
            }
            px += dx;
            py += dy;
        }
        px = Math.max(30f, Math.min(W - 30f, px));
        py = Math.max(120f, Math.min(H - 40f, py));

        // 引擎粒子
        engineT += dt;
        if (engineT > 0.03f) {
            engineT = 0;
            Particle p = new Particle();
            p.x = px + (rnd.nextFloat() - 0.5f) * 16f;
            p.y = py + 40f;
            p.vx = (rnd.nextFloat() - 0.5f) * 40f;
            p.vy = 260f + rnd.nextFloat() * 90f;
            p.life = 0.28f;
            p.maxLife = 0.28f;
            p.size = 9f;
            p.color = 0xFF59D6FF;
            p.drag = 0.9f;
            addParticle(p);
        }

        // 主武器
        fireT += dt;
        float interval = Math.max(0.045f, st.a.interval);
        while (fireT >= interval) {
            fireT -= interval;
            fireWeapon(st.a, false);
        }
        // 副武器：独立供弹计时
        if (st.b != null) {
            fireT2 += dt;
            float interval2 = Math.max(0.05f, st.b.interval);
            while (fireT2 >= interval2) {
                fireT2 -= interval2;
                fireWeapon(st.b, true);
            }
        }

        // 副炮
        if (st.sideGuns > 0) {
            sideT += dt;
            if (sideT >= 0.24f) {
                sideT = 0;
                for (int i = 1; i <= st.sideGuns; i++) {
                    float ox = 26f * i;
                    shootPlayer(px - ox, py + 6f, 0f, -st.bulletSpeed, st.damage * 0.55f, 0, 0);
                    shootPlayer(px + ox, py + 6f, 0f, -st.bulletSpeed, st.damage * 0.55f, 0, 0);
                }
            }
        }

        // 能量
        energy = Math.min(maxEnergy, energy + dt * 4.6f);
    }

    /** 通用开火：主武器与副武器共用同一套弹道生成 */
    private void fireWeapon(Data.Wpn w, boolean secondary) {
        float by = py - 26f;
        if (w.type == 0) {
            int n = w.streams;
            for (int i = 0; i < n; i++) {
                float off = n == 1 ? 0f : (i - (n - 1) / 2f) * 17f;
                float x = px + off + (secondary ? 16f : 0f);
                Bullet b = new Bullet();
                b.x = x;
                b.y = by;
                b.vx = 0;
                b.vy = -w.speed;
                b.dmg = w.damage;
                b.kind = 0;
                b.pierce = w.vulcanPierce;
                b.r = 8f;
                pBullets.add(b);
            }
            Sfx.play(Sfx.SHOOT, secondary ? 0.22f : 0.34f);
        } else if (w.type == 1) {
            Bullet b = new Bullet();
            b.x = px + (secondary ? 14f : 0f);
            b.y = py - 30f;
            b.vx = 0;
            b.vy = -w.speed;
            b.dmg = w.damage;
            b.kind = 1;
            b.pierce = w.pierce;
            b.r = 11f;
            b.split = w.laserSplit;
            pBullets.add(b);
            Sfx.play(Sfx.LASER, secondary ? 0.20f : 0.30f);
        } else if (w.type == 2) {
            int n = w.spread;
            float spreadDeg = Math.min(96f, 46f + n * 2.4f);
            for (int i = 0; i < n; i++) {
                float a = -spreadDeg / 2f + spreadDeg * (n == 1 ? 0.5f : i / (float) (n - 1));
                float rad = (float) Math.toRadians(a);
                Bullet b = new Bullet();
                b.x = px + (secondary ? 14f : 0f);
                b.y = py - 22f;
                b.vx = (float) Math.sin(rad) * w.speed * 0.92f;
                b.vy = -(float) Math.cos(rad) * w.speed * 0.92f;
                b.dmg = w.damage;
                b.kind = 2;
                b.r = 9f;
                b.angle = a;
                pBullets.add(b);
            }
            Sfx.play(Sfx.SHOOT, secondary ? 0.22f : 0.28f);
        } else {
            // 追踪导弹：扇形抛出后自动锁定
            int n = w.missiles;
            for (int i = 0; i < n; i++) {
                float off = (i - (n - 1) / 2f) * 22f;
                float a = (float) Math.toRadians(off * 1.8f);
                Bullet b = new Bullet();
                b.x = px + off * 0.6f + (secondary ? 14f : 0f);
                b.y = py + 4f;
                b.vx = (float) Math.sin(a) * 250f;
                b.vy = -(float) Math.cos(a) * 280f;
                b.dmg = w.damage;
                b.kind = 5;
                b.r = 15f;
                b.turn = 8.2f;
                b.accel = 420f;
                b.aoe = true;
                b.angle = off * 1.8f;
                b.target = nearestTargetId(b.x, b.y);
                pBullets.add(b);
            }
            Sfx.play(Sfx.LASER, secondary ? 0.24f : 0.34f);
        }
    }

    /** 最近的可锁定目标 */
    private int nearestTargetId(float x, float y) {
        int best = -1;
        float bd = Float.MAX_VALUE;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.y < -40f) continue;
            float dx = e.x - x, dy = e.y - y;
            // 优先锁定上方目标
            float d = dx * dx + dy * dy + (dy > 0 ? 90000f : 0f);
            if (d < bd) {
                bd = d;
                best = e.id;
            }
        }
        return best;
    }

    private Enemy findEnemy(int id) {
        if (id < 0) return null;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.id == id) return e;
        }
        return null;
    }

    private void shootPlayer(float x, float y, float vx, float vy, float dmg, int kind, int pierce) {
        Bullet b = new Bullet();
        b.x = x;
        b.y = y;
        b.vx = vx;
        b.vy = vy;
        b.dmg = dmg;
        b.kind = kind;
        b.pierce = pierce;
        b.r = 8f;
        pBullets.add(b);
    }

    // ---------------- 生成敌人 ----------------
    private void updateSpawning(float dt) {
        float duration = Data.stageDuration(stage);
        if (!bossSpawned && stageTime >= duration) {
            bossSpawned = true;
            bossWarnT = 2.2f;
            spawnT = 0;
            Sfx.play(Sfx.BOSS, 0.7f);
            return;
        }
        if (bossSpawned) {
            if (stageTime >= duration + 2.6f && boss == null && !cleared && !failed) {
                spawnBoss();
            }
            return;
        }
        spawnT -= dt;
        if (spawnT <= 0) {
            spawnT = Math.max(0.72f, 1.95f - stage * 0.075f) * (0.75f + rnd.nextFloat() * 0.6f);
            spawnWave();
        }
    }

    /** 按关卡解锁的敌机池，越后期越凶 */
    private int pickWaveType() {
        int[] pool;
        if (stage <= 1) {
            pool = new int[]{0, 0, 0, 1, 1, 2, 3};
        } else if (stage <= 3) {
            pool = new int[]{0, 0, 1, 1, 2, 2, 3, 4, 4, 7};
        } else if (stage <= 6) {
            pool = new int[]{0, 1, 1, 2, 2, 3, 4, 4, 5, 6, 7};
        } else {
            pool = new int[]{0, 1, 2, 2, 3, 4, 4, 5, 5, 6, 6, 7};
        }
        return pool[rnd.nextInt(pool.length)];
    }

    private void spawnWave() {
        float scaleHp = Data.enemyScale(stage);
        int type = pickWaveType();
        if (type == 0) {
            // V 字编队
            int n = 3 + rnd.nextInt(3);
            float cx = 120f + rnd.nextFloat() * (W - 240f);
            for (int i = 0; i < n; i++) {
                float off = (i - (n - 1) / 2f) * 46f;
                Enemy e = makeEnemy(E_SCOUT, cx + off, -60f - Math.abs(off) * 0.6f);
                e.hp = e.maxHp = 16f * scaleHp;
                e.vy = 150f + stage * 4f;
                enemies.add(e);
            }
        } else if (type == 1) {
            // 斜向掠过
            boolean fromLeft = rnd.nextBoolean();
            int n = 3 + rnd.nextInt(3);
            for (int i = 0; i < n; i++) {
                Enemy e = makeEnemy(E_ZIG, fromLeft ? -50f - i * 55f : W + 50f + i * 55f, 90f + rnd.nextFloat() * 130f);
                e.hp = e.maxHp = 22f * scaleHp;
                e.vx = (fromLeft ? 1 : -1) * (110f + stage * 3f);
                e.vy = 44f;
                e.baseX = e.x;
                e.amp = 26f;
                e.freq = 3.2f;
                enemies.add(e);
            }
        } else if (type == 2) {
            // 炮台机 + 护卫
            Enemy e = makeEnemy(E_TANK, 110f + rnd.nextFloat() * (W - 220f), -90f);
            e.hp = e.maxHp = 90f * scaleHp;
            e.vy = 74f;
            e.capY = 150f + rnd.nextFloat() * 130f;
            e.fireT = 0.8f;
            enemies.add(e);
            for (int i = 0; i < 2; i++) {
                Enemy s2 = makeEnemy(E_SCOUT, e.x + (i == 0 ? -60f : 60f), -140f);
                s2.hp = s2.maxHp = 14f * scaleHp;
                s2.vy = 140f;
                enemies.add(s2);
            }
        } else if (type == 3) {
            // 自杀冲撞机
            int n = 2 + rnd.nextInt(3);
            for (int i = 0; i < n; i++) {
                Enemy e = makeEnemy(E_KAM, 80f + rnd.nextFloat() * (W - 160f), -70f - i * 40f);
                e.hp = e.maxHp = 20f * scaleHp;
                e.aimX = px;
                e.aimY = py;
                e.vy = 90f;
                enemies.add(e);
            }
        } else if (type == 4) {
            // 护盾护卫机：皮厚，定期环形弹幕
            int n = 1 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) {
                Enemy e = makeEnemy(E_SHIELD, 100f + rnd.nextFloat() * (W - 200f), -80f - i * 90f);
                e.hp = e.maxHp = 55f * scaleHp;
                e.guard = 40f * scaleHp;
                e.vy = 82f + stage * 2f;
                e.fireT = 1.4f;
                enemies.add(e);
            }
        } else if (type == 5) {
            // 分裂母机：击毁后炸成 3 架蜂群
            Enemy e = makeEnemy(E_SPLIT, 130f + rnd.nextFloat() * (W - 260f), -100f);
            e.hp = e.maxHp = 110f * scaleHp;
            e.vy = 60f;
            e.fireT = 1.8f;
            enemies.add(e);
        } else if (type == 6) {
            // 狙击机：滞空蓄力后发射高速弹
            int n = 1 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) {
                Enemy e = makeEnemy(E_SNIPE, 90f + rnd.nextFloat() * (W - 180f), -70f - i * 70f);
                e.hp = e.maxHp = 45f * scaleHp;
                e.vy = 130f;
                e.capY = 110f + rnd.nextFloat() * 120f;
                e.baseX = e.x;
                e.aimX = px;
                e.fireT = 1.6f + i * 0.5f;
                enemies.add(e);
            }
        } else {
            // 蜂群：数量多、血量低
            int n = 5 + rnd.nextInt(4);
            float cx = 100f + rnd.nextFloat() * (W - 200f);
            for (int i = 0; i < n; i++) {
                Enemy e = makeEnemy(E_SWARM, cx + (rnd.nextFloat() - 0.5f) * 150f, -50f - i * 34f);
                e.hp = e.maxHp = 9f * scaleHp;
                e.vy = 190f + stage * 4f;
                e.baseX = rnd.nextFloat() * 6.28f;
                enemies.add(e);
            }
        }
    }

    private Enemy makeEnemy(int type, float x, float y) {
        Enemy e = new Enemy();
        e.type = type;
        e.id = ++idSeq;
        e.x = x;
        e.y = y;
        e.t = 0;
        e.r = type == E_TANK ? 42f : type == E_SPLIT ? 46f : type == E_SHIELD ? 36f
                : type == E_ZIG ? 28f : type == E_SNIPE ? 26f : type == E_SWARM ? 17f : 24f;
        e.vy = 140f;
        return e;
    }

    private void spawnBoss() {
        Enemy e = new Enemy();
        e.type = E_BOSS;
        e.id = ++idSeq;
        e.x = W / 2f;
        e.y = -190f;
        e.maxHp = e.hp = Data.bossHp(stage);
        e.r = 150f;
        e.capY = 220f;
        e.pattern = 0;
        e.fireT = 1.6f;
        boss = e;
        enemies.add(e);
    }

    // ---------------- 敌人 ----------------
    private void updateEnemies(float dt) {
        float slow = empTime > 0 ? 0.45f : 1f;
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.t += dt;
            if (e.hitFlash > 0) e.hitFlash = Math.max(0f, e.hitFlash - dt * 5f);

            switch (e.type) {
                case E_SCOUT:
                    e.y += e.vy * dt * slow;
                    e.x += (float) Math.sin(e.t * 2.4f) * 26f * dt;
                    break;
                case E_ZIG:
                    e.y += e.vy * dt * slow;
                    e.x += e.vx * dt * slow;
                    e.y += (float) Math.cos(e.t * e.freq) * 42f * dt * slow;
                    e.aimY = e.y;
                    e.fireT -= dt * slow;
                    if (e.fireT <= 0 && e.y > 40 && e.y < H * 0.7f) {
                        e.fireT = 2.0f + rnd.nextFloat() * 1.2f;
                        enemyAimShot(e.x, e.y, 250f + stage * 8f, 3, 0);
                    }
                    break;
                case E_TANK:
                    if (e.y < e.capY) {
                        e.y += e.vy * dt * slow;
                    } else if (e.t > 9.5f) {
                        e.y -= 130f * dt * slow;
                    } else {
                        e.x += (float) Math.sin(e.t * 1.1f) * 60f * dt * slow;
                        e.fireT -= dt * slow;
                        if (e.fireT <= 0) {
                            e.fireT = Math.max(1.05f, 1.9f - stage * 0.06f);
                            enemyFan(e.x, e.y + 26f, 3, 34f, 210f + stage * 7f);
                        }
                    }
                    break;
                case E_KAM:
                    float dx = e.aimX - e.x, dy = e.aimY - e.y;
                    float d = (float) Math.sqrt(dx * dx + dy * dy) + 0.01f;
                    e.vx = dx / d * (150f + e.t * 120f);
                    e.vy = dy / d * (150f + e.t * 120f);
                    e.x += e.vx * dt * slow;
                    e.y += e.vy * dt * slow;
                    break;
                case E_SHIELD:
                    e.y += e.vy * dt * slow;
                    e.x += (float) Math.sin(e.t * 1.4f) * 52f * dt * slow;
                    e.fireT -= dt * slow;
                    if (e.fireT <= 0 && e.y > 60f && e.y < H * 0.72f) {
                        e.fireT = 2.5f;
                        for (int k = 0; k < 8; k++) {
                            float a = (float) (Math.PI * 2 * k / 8 + e.t);
                            eBullets.add(makeEBullet(e.x, e.y,
                                    (float) Math.cos(a) * 150f, (float) Math.sin(a) * 150f + 70f));
                        }
                    }
                    break;
                case E_SPLIT:
                    e.y += e.vy * dt * slow;
                    e.x += (float) Math.sin(e.t * 0.85f) * 78f * dt * slow;
                    e.fireT -= dt * slow;
                    if (e.fireT <= 0 && e.y > 50f) {
                        e.fireT = 2.2f;
                        enemyFan(e.x, e.y + 30f, 5, 40f, 175f + stage * 6f);
                    }
                    break;
                case E_SNIPE:
                    if (e.y < e.capY) {
                        e.y += e.vy * dt * slow;
                    } else if (e.charge > 0) {
                        // 蓄力：缓慢跟随玩家
                        e.charge -= dt * slow;
                        e.aimX += (px - e.aimX) * Math.min(1f, dt * 1.8f);
                        e.aimY = py;
                        if (e.charge <= 0) {
                            float sx = e.aimX - e.x, sy = e.aimY - e.y;
                            float dd = (float) Math.sqrt(sx * sx + sy * sy) + 0.01f;
                            float sp = 460f + stage * 12f;
                            Bullet bb = makeEBullet(e.x, e.y + 20f, sx / dd * sp, sy / dd * sp);
                            bb.kind = 4;
                            bb.r = 12f;
                            eBullets.add(bb);
                            Sfx.play(Sfx.LASER, 0.30f);
                            e.fireT = 2.3f;
                            e.charge = 0;
                        }
                    } else if (e.t > 12f) {
                        e.y -= 120f * dt * slow;
                    } else {
                        e.x += (e.baseX - e.x) * Math.min(1f, dt * 0.7f);
                        e.fireT -= dt * slow;
                        if (e.fireT <= 0) e.charge = 0.95f;
                    }
                    break;
                case E_SWARM:
                    e.y += e.vy * dt * slow;
                    e.x += (float) Math.sin(e.t * 4.0f + e.baseX) * 110f * dt * slow;
                    break;
                case E_BOSS:
                    updateBoss(e, dt, slow);
                    break;
            }

            // 出界移除
            if (e.type != E_BOSS) {
                if (e.y > H + 90f || e.y < -220f || e.x < -180f || e.x > W + 180f) {
                    enemies.remove(i);
                }
            }
        }
    }

    private void updateBoss(Enemy e, float dt, float slow) {
        if (e.y < e.capY) {
            e.y += 120f * dt;
            return;
        }
        e.x = W / 2f + (float) Math.sin(e.t * 0.62f) * (W * 0.24f);
        e.y = e.capY + (float) Math.sin(e.t * 1.7f) * 12f;

        float hpPct = e.hp / e.maxHp;
        float rate = hpPct < 0.5f ? 1.35f : 1f;
        e.fireT -= dt * slow * rate;
        if (e.fireT <= 0) {
            int pat = e.pattern % 4;
            if (hpPct < 0.5f) pat = e.pattern % 5;
            switch (pat) {
                case 0:
                    enemyFan(e.x, e.y + 80f, 11, 26f, 200f + stage * 6f);
                    e.fireT = 1.5f;
                    break;
                case 1:
                    for (int k = -1; k <= 1; k += 2) {
                        enemyFan(e.x + k * 118f, e.y + 100f, 3, 22f, 240f + stage * 6f);
                    }
                    e.fireT = 1.15f;
                    break;
                case 2:
                    // 螺旋
                    for (int k = 0; k < 14; k++) {
                        float a = e.t * 1.6f + k * (float) (Math.PI * 2 / 14);
                        Bullet b = makeEBullet(e.x, e.y + 40f, (float) Math.cos(a) * 170f, 130f + (float) Math.sin(a) * 90f);
                        eBullets.add(b);
                    }
                    e.fireT = 1.8f;
                    break;
                case 3:
                    enemyAimShot(e.x, e.y + 60f, 320f + stage * 8f, 5, 0.16f);
                    e.fireT = 1.0f;
                    break;
                default:
                    // 狂暴：大弹
                    for (int k = 0; k < 8; k++) {
                        float a = (float) (k * Math.PI * 2 / 8 + e.t);
                        Bullet b = makeEBullet(e.x, e.y + 50f, (float) Math.cos(a) * 150f, (float) Math.sin(a) * 120f + 90f);
                        b.kind = 4;
                        b.r = 13f;
                        eBullets.add(b);
                    }
                    e.fireT = 1.2f;
                    break;
            }
            e.pattern++;
        }
    }

    private Bullet makeEBullet(float x, float y, float vx, float vy) {
        Bullet b = new Bullet();
        b.x = x;
        b.y = y;
        b.vx = vx;
        b.vy = vy;
        b.kind = 3;
        b.r = 7f;
        b.dmg = 0;
        return b;
    }

    private void enemyAimShot(float x, float y, float speed, int count, float gapSec) {
        float dx = px - x, dy = py - y;
        float d = (float) Math.sqrt(dx * dx + dy * dy) + 0.01f;
        for (int i = 0; i < count; i++) {
            float sp = speed * (0.82f + i * 0.06f);
            Bullet b = makeEBullet(x + (i - count / 2f) * 10f, y, dx / d * sp, dy / d * sp);
            eBullets.add(b);
        }
    }

    private void enemyFan(float x, float y, int count, float spreadDeg, float speed) {
        for (int i = 0; i < count; i++) {
            float a = 90f - spreadDeg / 2f + spreadDeg * (count == 1 ? 0.5f : i / (float) (count - 1));
            float rad = (float) Math.toRadians(a);
            eBullets.add(makeEBullet(x, y, (float) Math.cos(rad) * speed, (float) Math.sin(rad) * speed));
        }
    }

    // ---------------- 子弹 ----------------
    private void updateBullets(float dt) {
        for (int i = pBullets.size() - 1; i >= 0; i--) {
            Bullet b = pBullets.get(i);
            if (b.kind == 5) {
                updateMissile(b, dt);
            } else {
                b.x += b.vx * dt;
                b.y += b.vy * dt;
            }
            b.life += dt;
            if (b.kind == 5) {
                if (b.y < -70 || b.y > H + 70 || b.x < -70 || b.x > W + 70 || b.life > 2.8f) {
                    pBullets.remove(i);
                }
            } else if (b.y < -60 || b.y > H + 60 || b.x < -60 || b.x > W + 60 || b.life > 3f) {
                pBullets.remove(i);
            }
        }
        for (int i = eBullets.size() - 1; i >= 0; i--) {
            Bullet b = eBullets.get(i);
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.life += dt;
            if (b.y < -80 || b.y > H + 80 || b.x < -80 || b.x > W + 80 || b.life > 8f) {
                eBullets.remove(i);
            }
        }
    }

    /** 追踪导弹：朝目标持续转向并加速 */
    private void updateMissile(Bullet b, float dt) {
        Enemy tg = findEnemy(b.target);
        if (tg == null) {
            b.target = nearestTargetId(b.x, b.y);
            tg = findEnemy(b.target);
        }
        float spd = (float) Math.sqrt(b.vx * b.vx + b.vy * b.vy);
        spd = Math.min(1120f, spd + b.accel * dt);
        float cur = (float) Math.atan2(b.vy, b.vx);
        float want = (float) (-Math.PI / 2);
        if (tg != null) {
            want = (float) Math.atan2(tg.y - b.y, tg.x - b.x);
        }
        float diff = want - cur;
        while (diff > Math.PI) diff -= (float) (Math.PI * 2);
        while (diff < -Math.PI) diff += (float) (Math.PI * 2);
        float maxT = b.turn * dt;
        if (diff > maxT) diff = maxT;
        if (diff < -maxT) diff = -maxT;
        float na = cur + diff;
        b.vx = (float) Math.cos(na) * spd;
        b.vy = (float) Math.sin(na) * spd;
        b.angle = (float) Math.toDegrees(na) + 90f;
        b.x += b.vx * dt;
        b.y += b.vy * dt;

        // 尾焰烟迹
        Particle p = new Particle();
        p.x = b.x - b.vx * 0.012f;
        p.y = b.y - b.vy * 0.012f;
        p.vx = -b.vx * 0.10f + (rnd.nextFloat() - 0.5f) * 40f;
        p.vy = -b.vy * 0.10f + (rnd.nextFloat() - 0.5f) * 40f;
        p.life = 0.26f;
        p.maxLife = 0.26f;
        p.size = 7f;
        p.color = 0xFFFFA24A;
        p.drag = 0.9f;
        addParticle(p);
    }

    private void updateParticles(float dt) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            Particle p = parts.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                parts.remove(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= (1f - (1f - p.drag) * dt * 12f);
            p.vy *= (1f - (1f - p.drag) * dt * 12f);
        }
        if (parts.size() > 420) {
            for (int i = parts.size() - 1; i >= 0 && parts.size() > 420; i--) parts.remove(i);
        }
    }

    private void updateTexts(float dt) {
        for (int i = texts.size() - 1; i >= 0; i--) {
            FText f = texts.get(i);
            f.life -= dt;
            f.y -= 46f * dt;
            if (f.life <= 0) texts.remove(i);
        }
    }

    private void updatePowers(float dt) {
        for (int i = powers.size() - 1; i >= 0; i--) {
            Power p = powers.get(i);
            p.t += dt;
            float dx = px - p.x, dy = py - p.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy) + 0.01f;
            if (d < st.pickRadius * 1.9f) {
                p.x += dx / d * 460f * dt;
                p.y += dy / d * 460f * dt;
            } else {
                p.y += 130f * dt;
            }
            if (d < 34f) {
                collect(p);
                powers.remove(i);
                continue;
            }
            if (p.y > H + 40f) powers.remove(i);
        }
    }

    private void collect(Power p) {
        if (p.type == P_COIN) {
            int g = 12;
            earned += g;
            score += 40;
            addText("+" + g, p.x, p.y, UI.GOLD, 22f);
            Sfx.play(Sfx.COIN, 0.45f);
        } else if (p.type == P_EN) {
            energy = Math.min(maxEnergy, energy + 22f);
            addText("能量 +22", p.x, p.y, UI.CY, 20f);
            Sfx.play(Sfx.POWER, 0.4f);
        } else if (p.type == P_SHIELD) {
            shield = Math.min(maxShield, shield + 26f);
            addText("护盾 +26", p.x, p.y, 0xFF9BD0FF, 20f);
            Sfx.play(Sfx.POWER, 0.4f);
        } else {
            hp = Math.min(maxHp, hp + 24f);
            addText("装甲 +24", p.x, p.y, 0xFF55E08C, 20f);
            Sfx.play(Sfx.POWER, 0.4f);
        }
    }

    private void addText(String s, float x, float y, int color, float size) {
        FText f = new FText();
        f.s = s;
        f.x = x;
        f.y = y;
        f.color = color;
        f.size = size;
        f.life = 0.85f;
        texts.add(f);
    }

    private void addParticle(Particle p) {
        if (parts.size() < 460) parts.add(p);
    }

    private void burst(float x, float y, int count, float speed, float size, int color, float life) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            float a = rnd.nextFloat() * (float) (Math.PI * 2);
            float s = speed * (0.35f + rnd.nextFloat());
            p.x = x;
            p.y = y;
            p.vx = (float) Math.cos(a) * s;
            p.vy = (float) Math.sin(a) * s;
            p.life = life * (0.6f + rnd.nextFloat() * 0.7f);
            p.maxLife = p.life;
            p.size = size * (0.6f + rnd.nextFloat() * 0.8f);
            p.color = color;
            p.drag = 0.94f;
            addParticle(p);
        }
    }

    // ---------------- 碰撞 ----------------
    private void checkCollisions() {
        // 玩家子弹 vs 敌人（用快照遍历，避免命中溅射时边遍历边删）
        Enemy[] snap = enemies.toArray(new Enemy[0]);
        for (int i = pBullets.size() - 1; i >= 0; i--) {
            Bullet b = pBullets.get(i);
            boolean hit = false;
            for (int j = 0; j < snap.length; j++) {
                Enemy e = snap[j];
                if (e.hp <= 0 || enemies.indexOf(e) < 0) continue;
                if (!hitsEnemy(b, e)) continue;
                if (b.lastHit == e.id) continue;
                b.lastHit = e.id;
                damageEnemy(e, b.dmg, b.x, b.y);
                if (b.aoe) {
                    splash(b.x, b.y, 68f, b.dmg * 0.55f, e);
                    burst(b.x, b.y, 14, 240f, 11f, 0xFFFFB060, 0.45f);
                    Sfx.play(Sfx.EXPLODE, 0.24f);
                }
                if (b.split) splitLaser(b);
                if (b.pierce > 0) {
                    b.pierce--;
                    continue;
                }
                hit = true;
                break;
            }
            if (hit) pBullets.remove(i);
        }

        // 敌弹 vs 玩家
        if (invuln <= 0 && !failed && !cleared) {
            for (int i = eBullets.size() - 1; i >= 0; i--) {
                Bullet b = eBullets.get(i);
                float dx = b.x - px, dy = b.y - py;
                float rr = b.r + 15f;
                if (dx * dx + dy * dy < rr * rr) {
                    eBullets.remove(i);
                    hurtPlayer(b.kind == 4 ? 24f : 14f);
                }
            }
        }

        // 敌机撞击
        if (invuln <= 0 && !failed && !cleared) {
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                float dx = e.x - px, dy = e.y - py;
                float rr = (e.type == E_BOSS ? 120f : e.r * 0.8f) + 16f;
                if (dx * dx + dy * dy < rr * rr) {
                    if (e.type == E_BOSS) {
                        hurtPlayer(30f);
                    } else {
                        damageEnemy(e, 40f, e.x, e.y);
                        hurtPlayer(20f);
                    }
                    break;
                }
            }
        }
    }

    /** BOSS 用矩形碰撞盒，普通敌机用小圆 */
    private boolean hitsEnemy(Bullet b, Enemy e) {
        if (e.type == E_BOSS) {
            return Math.abs(b.x - e.x) < 198f + b.r && Math.abs(b.y - e.y) < 106f + b.r;
        }
        float rr = e.r + b.r;
        float dx = e.x - b.x, dy = e.y - b.y;
        return dx * dx + dy * dy < rr * rr;
    }

    private void damageEnemy(Enemy e, float dmg, float hx, float hy) {
        float d = dmg;
        if (e.guard > 0) {
            float use = Math.min(e.guard, d);
            e.guard -= use;
            d -= use;
            burst(hx, hy, 4, 130f, 6f, 0xFF7FE8FF, 0.26f);
            if (e.guard <= 0) Sfx.play(Sfx.HIT, 0.4f);
        }
        e.hp -= d;
        e.hitFlash = 1f;
        if (rnd.nextFloat() < 0.35f) {
            burst(hx, hy, 3, 90f, 5f, 0xFFFFD9A0, 0.22f);
        }
        if (e.hp <= 0) {
            killEnemy(e);
        }
    }

    /** 只扣血不处理死亡，供溅射使用 */
    private void hitHp(Enemy e, float dmg) {
        float d = dmg;
        if (e.guard > 0) {
            float use = Math.min(e.guard, d);
            e.guard -= use;
            d -= use;
        }
        e.hp -= d;
        e.hitFlash = 1f;
    }

    /** 导弹爆炸溅射 */
    private void splash(float x, float y, float r, float dmg, Enemy skip) {
        Enemy[] snap = enemies.toArray(new Enemy[0]);
        for (int i = 0; i < snap.length; i++) {
            Enemy e = snap[i];
            if (e == skip || e.hp <= 0) continue;
            float dx = e.x - x, dy = e.y - y;
            if (dx * dx + dy * dy < r * r) hitHp(e, dmg);
        }
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (e.hp <= 0) killEnemy(e);
        }
    }

    /** 激光裂变：命中后炸出三发散射弹 */
    private void splitLaser(Bullet b) {
        for (int k = -1; k <= 1; k++) {
            float a = (float) Math.toRadians(k * 34f);
            Bullet nb = new Bullet();
            nb.x = b.x;
            nb.y = b.y;
            nb.vx = (float) Math.sin(a) * 900f;
            nb.vy = -(float) Math.cos(a) * 900f;
            nb.dmg = b.dmg * 0.55f;
            nb.kind = 2;
            nb.r = 8f;
            nb.angle = k * 34f;
            pBullets.add(nb);
        }
        burst(b.x, b.y, 6, 160f, 7f, 0xFFB6F4FF, 0.3f);
    }

    private void killEnemy(Enemy e) {
        int idx = enemies.indexOf(e);
        if (idx < 0) return;
        enemies.remove(idx);
        if (e.type == E_BOSS) {
            boss = null;
            onBossKilled();
            return;
        }
        kills++;
        int coinBase;
        int scBase;
        switch (e.type) {
            case E_TANK: coinBase = 26; scBase = 260; break;
            case E_SPLIT: coinBase = 32; scBase = 300; break;
            case E_SHIELD: coinBase = 24; scBase = 240; break;
            case E_ZIG: coinBase = 14; scBase = 120; break;
            case E_KAM: coinBase = 12; scBase = 110; break;
            case E_SNIPE: coinBase = 18; scBase = 200; break;
            case E_SWARM: coinBase = 7; scBase = 60; break;
            default: coinBase = 9; scBase = 100; break;
        }
        int gold = Math.max(1, Math.round(coinBase * st.goldMul));
        earned += gold;
        score += scBase;
        energy = Math.min(maxEnergy, energy + 3.4f);

        boolean big = e.type == E_TANK || e.type == E_SPLIT || e.type == E_SHIELD;
        burst(e.x, e.y, big ? 26 : 16, 220f, 11f, 0xFFFFA13C, 0.55f);
        burst(e.x, e.y, 8, 120f, 8f, 0xFFFFF0C0, 0.35f);
        Sfx.play(Sfx.EXPLODE, big ? 0.5f : 0.32f);
        addText("+" + gold, e.x, e.y - 6f, UI.GOLD, 20f);

        // 分裂母机：炸出三架蜂群
        if (e.type == E_SPLIT) {
            float hpC = 10f * Math.max(1f, e.maxHp / 150f);
            for (int k = 0; k < 3; k++) {
                Enemy ch = makeEnemy(E_SWARM, e.x + (k - 1) * 36f, e.y - 4f);
                ch.hp = ch.maxHp = hpC;
                ch.vy = 175f + stage * 3f;
                ch.baseX = rnd.nextFloat() * 6.28f;
                enemies.add(ch);
            }
            addText("母舰解体！", e.x, e.y - 36f, 0xFFD6F06A, 19f);
        }

        // 掉落
        float roll = rnd.nextFloat();
        if (roll < 0.16f) {
            dropPower(e.x, e.y, rnd.nextFloat() < 0.45f ? P_COIN : P_EN);
        } else if (roll < 0.21f) {
            dropPower(e.x, e.y, rnd.nextFloat() < 0.5f ? P_SHIELD : P_REPAIR);
        }
    }

    private void dropPower(float x, float y, int type) {
        Power p = new Power();
        p.x = x;
        p.y = y;
        p.type = type;
        powers.add(p);
    }

    private void onBossKilled() {
        cleared = true;
        endSeq = true;
        endT = 0f;
        shake = 26f;
        nukeFlash = 0.7f;
        score += 2500 + stage * 400;
        earned += 120 + stage * 40;
        // 连锁爆炸
        for (int i = 0; i < 26; i++) {
            float a = rnd.nextFloat() * (float) (Math.PI * 2);
            float r = rnd.nextFloat() * 130f;
            float x = W / 2f + (float) Math.cos(a) * r;
            float y = 220f + (float) Math.sin(a) * r * 0.7f;
            burst(x, y, 7, 210f, 13f, i % 3 == 0 ? 0xFFFFE0A0 : 0xFFFF7A2C, 0.8f);
        }
        Sfx.play(Sfx.EXPLODE);
        Sfx.play(Sfx.DEATH, 0.7f);
        eBullets.clear();
    }

    private void hurtPlayer(float dmg) {
        if (invuln > 0 || failed || cleared) return;
        float remaining = dmg;
        if (shield > 0) {
            float use = Math.min(shield, remaining);
            shield -= use;
            remaining -= use;
        }
        if (remaining > 0) hp -= remaining;
        invuln = 0.9f;
        shake = 12f;
        burst(px, py, 12, 160f, 9f, 0xFF8FE8FF, 0.4f);
        Sfx.play(Sfx.HIT, 0.7f);
        if (hp <= 0) {
            hp = 0;
            failed = true;
            endSeq = true;
            endT = 0f;
            shake = 24f;
            burst(px, py, 40, 300f, 15f, 0xFFFF9A3C, 0.9f);
            burst(px, py, 20, 200f, 10f, 0xFFFFF0C0, 0.7f);
            Sfx.play(Sfx.DEATH);
        }
    }

    // ---------------- 超级武器 ----------------
    private void fireSuper() {
        if (energy < maxEnergy) {
            Sfx.play(Sfx.HIT, 0.4f);
            return;
        }
        energy = 0;
        Sfx.play(Sfx.SUP);
        shake = 18f;
        int lv = st.sLv;
        if (st.sType == 0) {
            nukeFlash = 1f;
            eBullets.clear();
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy e = enemies.get(i);
                damageEnemy(e, 220f + 90f * lv, e.x, e.y);
            }
            burst(W / 2f, H * 0.45f, 60, 520f, 20f, 0xFFFFE0A0, 1.1f);
            addText("战术核弹！", W / 2f, H * 0.4f, 0xFFFFD166, 40f);
        } else if (st.sType == 1) {
            empTime = 6f;
            eBullets.clear();
            burst(W / 2f, H * 0.45f, 40, 400f, 16f, 0xFF7FE8FF, 0.9f);
            addText("电磁风暴！", W / 2f, H * 0.4f, 0xFF7FE8FF, 40f);
        } else {
            wingTime = 14f;
            addText("僚机编队！", W / 2f, H * 0.4f, 0xFFB6F4FF, 40f);
            burst(px, py, 30, 300f, 14f, 0xFF7FE8FF, 0.7f);
        }
    }

    private void empTick(float dt) {
        float dmg = (14f + 6f * st.sLv) * dt;
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.hp -= dmg;
            if (rnd.nextFloat() < 0.06f) burst(e.x, e.y, 1, 60f, 7f, 0xFF7FE8FF, 0.3f);
            if (e.hp <= 0) killEnemy(e);
        }
        if (rnd.nextFloat() < 0.3f) {
            float x = rnd.nextFloat() * W;
            float y = rnd.nextFloat() * H;
            burst(x, y, 1, 40f, 9f, 0xFF9BF0FF, 0.5f);
        }
    }

    private void updateWingmen(float dt) {
        if (wingTime <= 0) return;
        int n = st.sLv >= 4 ? 4 : 2;
        // 用计数器节流
        wingFireT += dt;
        if (wingFireT < 0.19f) return;
        wingFireT = 0;
        for (int i = 0; i < n; i++) {
            float side = (i % 2 == 0) ? -1f : 1f;
            float dist = 58f + (i / 2) * 52f;
            float wx = px + side * dist;
            float wy = py - 6f + (i / 2) * 16f;
            shootPlayer(wx, wy - 18f, 0f, -st.bulletSpeed * 0.95f, st.damage * 0.7f, 0, 0);
        }
    }

    private float wingFireT;

    // ================= 绘制 =================
    @Override
    public void draw(Canvas c) {
        c.save();
        if (shake > 0.4f) {
            c.translate((rnd.nextFloat() - 0.5f) * shake, (rnd.nextFloat() - 0.5f) * shake);
        }

        // 背景
        float tile = Art.nebula.getHeight();
        float y = bgScroll % tile;
        c.drawBitmap(Art.nebula, 0, y - tile, P);
        c.drawBitmap(Art.nebula, 0, y, P);
        c.drawBitmap(Art.nebula, 0, y + tile, P);

        // 快速星线
        for (int i = 0; i < 40; i++) {
            float seed = i * 53.7f;
            float sx = (float) ((seed * 137.5) % W);
            float spd = 240f + (i % 6) * 90f;
            float sy = ((seed * 71.3f) + bgScroll * spd / 160f) % (H + 60) - 30;
            P.setColor(0x66CFE8FF);
            c.drawRect(sx, sy, sx + 1.6f, sy + 10f + (i % 4) * 4f, P);
        }

        // 掉落物
        drawPowers(c);
        // 敌人
        drawEnemies(c);
        // 玩家子弹
        drawPlayerBullets(c);
        // 僚机
        drawWingmen(c);
        // 玩家
        drawPlayer(c);
        // 敌弹
        drawEnemyBullets(c);
        // 粒子
        drawParticles(c);
        // 飘字
        for (int i = 0; i < texts.size(); i++) {
            FText f = texts.get(i);
            float a = Math.min(1f, f.life * 2.2f);
            UI.bold(c, f.s, f.x, f.y, f.size, ((int) (a * 255) << 24) | (f.color & 0xFFFFFF), 1);
        }

        c.restore();

        // 电磁滤镜
        if (empTime > 0) {
            P.setShader(new RadialGradient(W / 2f, H / 2f, W * 0.9f,
                    new int[]{0x00000000, 0x4480E0FF}, new float[]{0.4f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, W, H, P);
            P.setShader(null);
        }

        drawHud(c);

        if (nukeFlash > 0) {
            P.setColor(((int) (nukeFlash * 210) << 24) | 0xFFFFFF);
            c.drawRect(0, 0, W, H, P);
        }
        if (bossWarnT > 0) drawBossWarning(c);
        if (paused) drawPause(c);
        if (cleared) drawClearBanner(c);
        if (failed) drawFailBanner(c);
    }

    private void drawPlayer(Canvas c) {
        if (failed) return;
        boolean blink = invuln > 0 && ((int) (invuln * 14f) % 2 == 0);
        if (blink) return;
        float bob = (float) Math.sin(t * 5f) * 1.5f;
        c.save();
        c.translate(px, py + bob);
        c.drawBitmap(Art.player, -Art.player.getWidth() / 2f, -Art.player.getHeight() / 2f, P);
        c.restore();

        // 护盾环
        if (shield > 0) {
            P.setStyle(Paint.Style.STROKE);
            P.setStrokeWidth(2.2f);
            P.setColor(0x884FE0FF);
            c.drawCircle(px, py, 40f, P);
            P.setColor(0x334FE0FF);
            c.drawCircle(px, py, 45f, P);
            P.setStyle(Paint.Style.FILL);
        }
    }

    private void drawWingmen(Canvas c) {
        if (wingTime <= 0) return;
        int n = st.sLv >= 4 ? 4 : 2;
        for (int i = 0; i < n; i++) {
            float side = (i % 2 == 0) ? -1f : 1f;
            float dist = 58f + (i / 2) * 52f;
            float wx = px + side * dist;
            float wy = py - 6f + (i / 2) * 16f;
            c.save();
            c.translate(wx, wy);
            c.scale(0.55f, 0.55f);
            P.setAlpha(190);
            c.drawBitmap(Art.player, -Art.player.getWidth() / 2f, -Art.player.getHeight() / 2f, P);
            P.setAlpha(255);
            c.restore();
        }
    }

    private void drawEnemies(Canvas c) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.type == E_BOSS) {
                drawBoss(c, e);
                continue;
            }
            // 狙击机蓄力：画出瞄准预警线
            if (e.type == E_SNIPE && e.charge > 0) {
                float k = 1f - e.charge / 0.95f;
                int a = (int) (60 + 150 * k);
                P.setColor((a << 24) | 0xFF3355);
                P.setStrokeWidth(1.4f + 2.2f * k);
                c.drawLine(e.x, e.y, e.aimX, e.aimY, P);
                P.setColor((a << 24) | 0xFF6688);
                c.drawCircle(e.aimX, e.aimY, 12f + 10f * k, P);
                P.setStrokeWidth(1f);
            }

            float s = 0.84f;
            if (e.type == E_TANK) s = 0.92f;
            else if (e.type == E_ZIG) s = 0.86f;
            else if (e.type == E_SHIELD) s = 0.90f;
            else if (e.type == E_SPLIT) s = 0.96f;
            else if (e.type == E_SNIPE) s = 0.90f;
            else if (e.type == E_SWARM) s = 0.80f;

            c.save();
            c.translate(e.x, e.y);
            c.scale(s, s);
            if (e.type == E_KAM) {
                float ang = (float) Math.toDegrees(Math.atan2(e.vy, e.vx)) - 90f;
                c.rotate(ang * 0.6f);
            }
            android.graphics.Bitmap bm = sprite(e.type);
            if (bm != null) {
                c.drawBitmap(bm, -bm.getWidth() / 2f, -bm.getHeight() / 2f, P);
            }
            if (e.hitFlash > 0) {
                P.setAlpha((int) (e.hitFlash * 150));
                P.setColor(0xFFFFFFFF);
                c.drawCircle(0, 0, e.r * 0.9f, P);
                P.setAlpha(255);
            }
            c.restore();

            // 护盾机的能量护罩
            if (e.type == E_SHIELD && e.guard > 0) {
                float pr = 0.5f + 0.5f * (float) Math.sin(t * 6f);
                P.setStyle(Paint.Style.STROKE);
                P.setStrokeWidth(2.6f);
                P.setColor((int) (110 + 80 * pr) << 24 | 0x5BE6FF);
                c.drawCircle(e.x, e.y, e.r + 8f + pr * 3f, P);
                P.setColor(0x33AFF4FF);
                c.drawCircle(e.x, e.y, e.r + 4f, P);
                P.setStyle(Paint.Style.FILL);
            }
        }
    }

    private android.graphics.Bitmap sprite(int type) {
        switch (type) {
            case E_SCOUT:
                return Art.eScout;
            case E_ZIG:
                return Art.eZig;
            case E_TANK:
                return Art.eTank;
            case E_KAM:
                return Art.eKam;
            case E_SHIELD:
                return Art.eShield;
            case E_SPLIT:
                return Art.eSplit;
            case E_SNIPE:
                return Art.eSnipe;
            case E_SWARM:
                return Art.eSwarm;
            default:
                return Art.boss;
        }
    }

    private void drawBoss(Canvas c, Enemy e) {
        float sc = Math.min(1.05f, (W * 0.92f) / Art.boss.getWidth());
        c.save();
        c.translate(e.x, e.y);
        c.scale(sc, sc);
        c.drawBitmap(Art.boss, -Art.boss.getWidth() / 2f, -Art.boss.getHeight() / 2f, P);
        c.restore();
        // 核心脉冲
        float pulse = 0.6f + (float) Math.sin(t * 5f) * 0.25f;
        P.setShader(new RadialGradient(e.x, e.y + 10f, 70f,
                new int[]{(int) (150 * pulse) << 24 | 0xFF8A40, 0x00000000},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(e.x, e.y + 10f, 70f, P);
        P.setShader(null);
    }

    private void drawPlayerBullets(Canvas c) {
        for (int i = 0; i < pBullets.size(); i++) {
            Bullet b = pBullets.get(i);
            c.save();
            c.translate(b.x, b.y);
            android.graphics.Bitmap bm;
            if (b.kind == 1) bm = Art.bLaser;
            else if (b.kind == 2) bm = Art.bPlasma;
            else if (b.kind == 5) bm = Art.bMissile;
            else bm = Art.bVulcan;
            if (b.kind == 2) c.rotate(b.angle);
            else if (b.kind == 5) c.rotate(b.angle);
            c.drawBitmap(bm, -bm.getWidth() / 2f, -bm.getHeight() / 2f, P);
            c.restore();
        }
    }

    private void drawEnemyBullets(Canvas c) {
        for (int i = 0; i < eBullets.size(); i++) {
            Bullet b = eBullets.get(i);
            android.graphics.Bitmap bm = b.kind == 4 ? Art.bEnemyBig : Art.bEnemy;
            c.save();
            c.translate(b.x, b.y);
            float sc = b.kind == 4 ? 1.15f : 1f;
            c.scale(sc, sc);
            c.drawBitmap(bm, -bm.getWidth() / 2f, -bm.getHeight() / 2f, P);
            c.restore();
        }
    }

    private void drawParticles(Canvas c) {
        for (int i = 0; i < parts.size(); i++) {
            Particle p = parts.get(i);
            float k = p.life / p.maxLife;
            int a = (int) (Math.min(1f, k * 1.6f) * 255);
            int col = (p.color & 0xFFFFFF) | (a << 24);
            P.setColor(col);
            float sz = p.size * (0.35f + k * 0.9f);
            c.drawCircle(p.x, p.y, sz, P);
        }
    }

    private void drawPowers(Canvas c) {
        for (int i = 0; i < powers.size(); i++) {
            Power p = powers.get(i);
            float bob = (float) Math.sin(p.t * 6f) * 3f;
            int col;
            String glyph;
            if (p.type == P_COIN) {
                col = 0xFFFFCC44;
                glyph = "G";
            } else if (p.type == P_EN) {
                col = 0xFF4FE0FF;
                glyph = "E";
            } else if (p.type == P_SHIELD) {
                col = 0xFF9BD0FF;
                glyph = "S";
            } else {
                col = 0xFF55E08C;
                glyph = "H";
            }
            float x = p.x, y = p.y + bob;
            P.setShader(new RadialGradient(x, y, 30f, new int[]{(col & 0xFFFFFF) | 0x88000000, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, 30f, P);
            P.setShader(null);
            P.setColor(0xDD0A1420);
            c.drawCircle(x, y, 18f, P);
            P.setStyle(Paint.Style.STROKE);
            P.setStrokeWidth(2.4f);
            P.setColor(col);
            c.drawCircle(x, y, 18f, P);
            P.setStyle(Paint.Style.FILL);
            UI.bold(c, glyph, x, y + 8f, 22f, col, 1);
        }
    }

    private void drawHud(Canvas c) {
        // 装甲
        float bw = Math.min(212f, W * 0.31f);
        UI.bar(c, 22f, 40f, bw, 13f, hp / Math.max(1f, maxHp), 0xFF39D8FF, 0xFF7AF0FF);
        UI.text(c, (int) Math.ceil(hp) + "/" + (int) maxHp, 22f + bw + 8f, 51f, 13f, 0xFFA8C8DC, 0);
        if (maxShield > 0) {
            UI.bar(c, 22f, 57f, bw, 8f, shield / Math.max(1f, maxShield), 0xFF6E9BFF, 0xFFA8C8FF);
        }

        // 分数
        UI.bold(c, UI.num(score), W / 2f, 46f, 27f, 0xFFEAF6FF, 1);
        UI.text(c, "第 " + stage + " 关 · " + Data.stageTitle(stage), W / 2f, 70f, 14f, 0xFF8FA6BD, 1);

        // 进度 / BOSS 血条
        if (boss == null && !bossSpawned) {
            float prog = Math.min(1f, stageTime / Data.stageDuration(stage));
            UI.bar(c, 22f, 72f, bw, 5f, prog, 0xFF55E08C, 0xFF9BF0B0);
        } else if (boss != null) {
            float w = W - 150f;
            UI.bar(c, 75f, 88f, w, 11f, boss.hp / boss.maxHp, 0xFFFF5A4A, 0xFFFFB03C);
            UI.text(c, "BOSS", 75f + w / 2f, 114f, 13f, 0xFFFFD9A0, 1);
        }

        // 融合标记
        if (st.fused) {
            UI.text(c, "融合 · " + st.fusionName, W - 22f, 104f, 13f, 0xFFB6F4FF, 2);
        }

        // 暂停
        UI.fillRound(c, bPause.left, bPause.top, bPause.right, bPause.bottom, 12,
                pressed == 200 ? 0xEE2A4560 : 0x99142030);
        P.setColor(0xFFBFE0F0);
        c.drawRect(bPause.centerX() - 11, bPause.centerY() - 14, bPause.centerX() - 4, bPause.centerY() + 14, P);
        c.drawRect(bPause.centerX() + 4, bPause.centerY() - 14, bPause.centerX() + 11, bPause.centerY() + 14, P);

        // 超级武器按钮
        boolean ready = energy >= maxEnergy;
        float r = btnSuperR;
        P.setColor(0x99203040);
        c.drawCircle(btnSuperX, btnSuperY, r, P);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(7f);
        P.setColor(0xFF1B3A52);
        c.drawCircle(btnSuperX, btnSuperY, r - 6f, P);
        P.setColor(ready ? 0xFFFFC83C : UI.CY);
        RectF arc = new RectF(btnSuperX - r + 6f, btnSuperY - r + 6f, btnSuperX + r - 6f, btnSuperY + r - 6f);
        c.drawArc(arc, -90f, 360f * Math.min(1f, energy / maxEnergy), false, P);
        P.setStyle(Paint.Style.FILL);
        if (ready) {
            P.setShader(new RadialGradient(btnSuperX, btnSuperY, r,
                    new int[]{0x55FFC83C, 0x00FFC83C}, null, Shader.TileMode.CLAMP));
            c.drawCircle(btnSuperX, btnSuperY, r, P);
            P.setShader(null);
        }
        UI.bold(c, "超武", btnSuperX, btnSuperY - 4f, 20f, ready ? 0xFFFFE9A8 : 0xFF8FB4C8, 1);
        UI.text(c, ready ? "就绪" : (int) energy + "%", btnSuperX, btnSuperY + 22f, 15f,
                ready ? 0xFFFFD166 : 0xFF7C93A8, 1);
        if (ready) {
            UI.text(c, Data.S_NAME[st.sType], btnSuperX, btnSuperY - r - 14f, 15f, 0xFFFFD166, 1);
        }
    }

    private void drawBossWarning(Canvas c) {
        float a = Math.min(1f, bossWarnT / 0.4f);
        int alpha = (int) (200 * Math.min(1f, a));
        P.setColor((alpha << 24) | 0x1A0000);
        c.drawRect(0, H * 0.36f, W, H * 0.50f, P);
        P.setColor(((int) (alpha * 0.8f) << 24) | 0xFF3A2A);
        c.drawRect(0, H * 0.358f, W, H * 0.365f, P);
        c.drawRect(0, H * 0.495f, W, H * 0.502f, P);
        boolean on = ((int) (t * 6) % 2) == 0;
        if (on) UI.glowText(c, "警 告  BOSS 接 近", W / 2f, H * 0.455f, 42f, 0xFFFF6A5A, 0xFFFF2A1A);
    }

    private void drawClearBanner(Canvas c) {
        float a = Math.min(1f, endT / 0.5f);
        P.setColor((int) (170 * a) << 24);
        c.drawRect(0, 0, W, H, P);
        UI.glowText(c, "任 务 完 成", W / 2f, H * 0.42f, 52f, 0xFFB6FFD8, 0xFF2FFF9A);
        UI.text(c, "战斗中收获金币 " + UI.num(earned), W / 2f, H * 0.42f + 46f, 22f, 0xFFFFD166, 1);
    }

    private void drawFailBanner(Canvas c) {
        float a = Math.min(1f, endT / 0.6f);
        P.setColor((int) (190 * a) << 24);
        c.drawRect(0, 0, W, H, P);
        UI.glowText(c, "任 务 失 败", W / 2f, H * 0.42f, 50f, 0xFFFFB0A8, 0xFFFF3A2A);
        UI.text(c, "机体已被击坠 · 回收金币 " + UI.num(earned), W / 2f, H * 0.42f + 46f, 21f, 0xFF9FBAD2, 1);
    }

    private void drawPause(Canvas c) {
        P.setColor(0xD9000000);
        c.drawRect(0, 0, W, H, P);
        float pw = 460f;
        float pl = (W - pw) / 2f;
        float pt = H * 0.5f - 190f;
        UI.panel(c, pl, pt, pl + pw, pt + 390f);
        UI.bold(c, "暂 停", W / 2f, pt + 72f, 34f, UI.CY, 1);
        UI.button(c, bResume, "继 续 战 斗", true, pressed == 201, 0);
        UI.button(c, bRestart, "重 新 挑 战", true, pressed == 202, 2);
        UI.button(c, bQuit, "撤 退", true, pressed == 203, 3);
    }

    // ================= 输入 =================
    @Override
    public void onTouch(int action, float x, float y) {
        if (paused) {
            if (action == 0) {
                pressed = -1;
                if (bResume.contains(x, y)) pressed = 201;
                else if (bRestart.contains(x, y)) pressed = 202;
                else if (bQuit.contains(x, y)) pressed = 203;
                if (pressed > 0) Sfx.play(Sfx.UI);
            } else if (action == 2) {
                int id = pressed;
                pressed = -1;
                if (id == 201 && bResume.contains(x, y)) {
                    paused = false;
                    dragging = false;
                    Sfx.resumeMusic();
                } else if (id == 202 && bRestart.contains(x, y)) {
                    paused = false;
                    game.setScene(new BattleScene(stage));
                } else if (id == 203 && bQuit.contains(x, y)) {
                    game.save.earn(earned);
                    game.setScene(new MenuScene());
                }
            } else if (action == 3) {
                pressed = -1;
            }
            return;
        }
        if (cleared || failed) return;

        if (action == 4 || action == 5) {
            if (action == 4) {
                float dx = x - btnSuperX, dy = y - btnSuperY;
                if (dx * dx + dy * dy < (btnSuperR + 20f) * (btnSuperR + 20f)) fireSuper();
            }
            return;
        }

        if (action == 0) {
            if (bPause.contains(x, y)) {
                pressed = 200;
                Sfx.play(Sfx.UI);
                paused = true;
                Sfx.pauseMusic();
                return;
            }
            float dx = x - btnSuperX, dy = y - btnSuperY;
            if (dx * dx + dy * dy < (btnSuperR + 16f) * (btnSuperR + 16f)) {
                pressed = 210;
                fireSuper();
                return;
            }
            dragging = true;
            dragDX = px - x;
            dragDY = py - y;
            fingerX = x;
            fingerY = y;
        } else if (action == 1) {
            if (dragging) {
                fingerX = x;
                fingerY = y;
            }
        } else if (action == 2 || action == 3) {
            dragging = false;
            pressed = -1;
        }
    }

    @Override
    public boolean onBack() {
        if (cleared || failed) return true;
        if (paused) {
            paused = false;
            Sfx.resumeMusic();
            return true;
        }
        paused = true;
        Sfx.pauseMusic();
        return true;
    }
}
