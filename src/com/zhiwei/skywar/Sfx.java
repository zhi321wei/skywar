package com.zhiwei.skywar;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Looper;

public class Sfx {

    public static final int SHOOT = 0;
    public static final int LASER = 1;
    public static final int EXPLODE = 2;
    public static final int HIT = 3;
    public static final int POWER = 4;
    public static final int UI = 5;
    public static final int COIN = 6;
    public static final int SUP = 7;
    public static final int BOSS = 8;
    public static final int DEATH = 9;

    /** 菜单 / 机库 / 商店 / 结算用的舒缓曲 */
    public static final int TRACK_MENU = 0;
    /** 关卡内用的激昂曲 */
    public static final int TRACK_BATTLE = 1;

    private static final int[] TRACK_RES = {R.raw.bgm, R.raw.bgm_battle};
    private static final float[] TRACK_VOL = {0.42f, 0.50f};

    private static SoundPool pool;
    private static final int[] ids = new int[10];
    private static final MediaPlayer[] mps = new MediaPlayer[TRACK_RES.length];
    private static volatile int curTrack = TRACK_MENU;
    private static volatile boolean bgmReady;
    private static volatile boolean musicStarting;
    private static volatile boolean sfxOn = true;
    private static volatile boolean musicOn = true;
    private static long lastShoot;

    public static void init(Context c) {
        if (pool != null) return;
        try {
            pool = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);
            ids[SHOOT] = pool.load(c, R.raw.sfx_shoot, 1);
            ids[LASER] = pool.load(c, R.raw.sfx_laser, 1);
            ids[EXPLODE] = pool.load(c, R.raw.sfx_explode, 1);
            ids[HIT] = pool.load(c, R.raw.sfx_hit, 1);
            ids[POWER] = pool.load(c, R.raw.sfx_power, 1);
            ids[UI] = pool.load(c, R.raw.sfx_ui, 1);
            ids[COIN] = pool.load(c, R.raw.sfx_coin, 1);
            ids[SUP] = pool.load(c, R.raw.sfx_sup, 1);
            ids[BOSS] = pool.load(c, R.raw.sfx_boss, 1);
            ids[DEATH] = pool.load(c, R.raw.sfx_death, 1);
        } catch (Throwable e) {
            pool = null;
        }
        startMusic(c);
    }

    public static void setEnabled(boolean sfx, boolean music) {
        sfxOn = sfx;
        boolean wasMusic = musicOn;
        musicOn = music;
        if (music && !wasMusic) resumeMusic();
        if (!music) pauseMusic();
    }

    public static void play(int id) {
        play(id, 1f);
    }

    public static void play(int id, float vol) {
        if (!sfxOn || pool == null) return;
        if (id == SHOOT) {
            long now = System.currentTimeMillis();
            if (now - lastShoot < 55) return;
            lastShoot = now;
        }
        try {
            float rate = id == SHOOT ? (0.94f + (float) Math.random() * 0.14f) : 1f;
            pool.play(ids[id], vol, vol, 1, 0, rate);
        } catch (Throwable ignored) {
        }
    }

    /**
     * BGM 在独立线程（自带 Looper）里创建。
     * MediaPlayer.create 会同步解析整个音频文件，在主线程做的话，
     * 一旦某个机型解析慢，onCreate 就不返回 —— 用户看到的就是「打开一片黑」。
     *
     * 两首曲子都在这里一次建好，之后切关卡只是 pause/start，没有解析延迟。
     */
    public static void startMusic(final Context c) {
        if (bgmReady) {
            resumeMusic();
            return;
        }
        if (musicStarting) return;
        musicStarting = true;
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                for (int i = 0; i < TRACK_RES.length; i++) {
                    try {
                        MediaPlayer mp = MediaPlayer.create(c, TRACK_RES[i]);
                        if (mp != null) {
                            mp.setLooping(true);
                            mp.setVolume(TRACK_VOL[i], TRACK_VOL[i]);
                            mps[i] = mp;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                bgmReady = true;
                MediaPlayer cur = mps[curTrack];
                if (cur != null && musicOn) {
                    try {
                        cur.start();
                    } catch (Throwable ignored) {
                    }
                }
                Looper.loop();
            }
        }, "skywar-bgm");
        th.setDaemon(true);
        th.start();
    }

    /** 切曲目：暂停旧的、从头播放新的。未就绪时先记下目标，就绪后再播 */
    public static void setTrack(int idx) {
        if (idx < 0 || idx >= mps.length) return;
        if (idx == curTrack) return;
        int old = curTrack;
        curTrack = idx;
        MediaPlayer o = mps[old];
        if (o != null) {
            try {
                if (o.isPlaying()) o.pause();
            } catch (Throwable ignored) {
            }
        }
        MediaPlayer n = mps[idx];
        if (n != null && musicOn) {
            try {
                n.seekTo(0);
                n.start();
            } catch (Throwable ignored) {
            }
        }
    }

    public static int getTrack() {
        return curTrack;
    }

    public static void pauseMusic() {
        MediaPlayer mp = mps[curTrack];
        try {
            if (mp != null && mp.isPlaying()) mp.pause();
        } catch (Throwable ignored) {
        }
    }

    public static void resumeMusic() {
        MediaPlayer mp = mps[curTrack];
        try {
            if (mp != null && musicOn && !mp.isPlaying()) mp.start();
        } catch (Throwable ignored) {
        }
    }
}
