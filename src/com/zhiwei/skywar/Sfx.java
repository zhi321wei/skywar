package com.zhiwei.skywar;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;

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

    private static SoundPool pool;
    private static final int[] ids = new int[10];
    private static MediaPlayer bgm;
    private static boolean sfxOn = true;
    private static boolean musicOn = true;
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

    public static void startMusic(Context c) {
        if (bgm != null) {
            resumeMusic();
            return;
        }
        try {
            bgm = MediaPlayer.create(c, R.raw.bgm);
            if (bgm != null) {
                bgm.setLooping(true);
                bgm.setVolume(0.42f, 0.42f);
                if (musicOn) bgm.start();
            }
        } catch (Throwable e) {
            bgm = null;
        }
    }

    public static void pauseMusic() {
        try {
            if (bgm != null && bgm.isPlaying()) bgm.pause();
        } catch (Throwable ignored) {
        }
    }

    public static void resumeMusic() {
        try {
            if (bgm != null && musicOn && !bgm.isPlaying()) bgm.start();
        } catch (Throwable ignored) {
        }
    }
}
