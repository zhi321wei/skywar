package com.zhiwei.skywar;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public class Save {

    public static final String PREF = "skywar_save_v1";

    public int gold = 800;
    public int weapon = 0;
    public int[] weaponLv = {1, 0, 0};
    public int sup = 0;
    public int[] supLv = {1, 0, 0};
    public int[] partLv = {0, 0, 0, 0, 0};
    public int maxStage = 1;
    public int[] bestScore = new int[Data.STAGES];
    public int[] cons = {0, 0, 0};
    public int sfxOn = 1;
    public int musicOn = 1;
    public int totalKills = 0;
    public int totalGold = 0;
    public int clears = 0;

    private final SharedPreferences sp;

    public Save(Context c) {
        sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        String raw = sp.getString("data", null);
        if (raw == null) return;
        try {
            JSONObject o = new JSONObject(raw);
            gold = o.optInt("gold", gold);
            weapon = o.optInt("weapon", 0);
            sup = o.optInt("sup", 0);
            maxStage = o.optInt("maxStage", 1);
            sfxOn = o.optInt("sfx", 1);
            musicOn = o.optInt("music", 1);
            totalKills = o.optInt("kills", 0);
            totalGold = o.optInt("tgold", 0);
            clears = o.optInt("clears", 0);
            readArr(o.optJSONArray("wl"), weaponLv, 1);
            readArr(o.optJSONArray("sl"), supLv, 1);
            readArr(o.optJSONArray("pl"), partLv, 0);
            readArr(o.optJSONArray("cons"), cons, 0);
            readArr(o.optJSONArray("best"), bestScore, 0);
            if (weapon < 0 || weapon > 2) weapon = 0;
            if (sup < 0 || sup > 2) sup = 0;
            if (maxStage < 1) maxStage = 1;
            if (maxStage > Data.STAGES) maxStage = Data.STAGES;
            for (int i = 0; i < weaponLv.length; i++) {
                if (weaponLv[i] > Data.MAX_LV) weaponLv[i] = Data.MAX_LV;
                if (weaponLv[i] < 0) weaponLv[i] = 0;
            }
            for (int i = 0; i < supLv.length; i++) {
                if (supLv[i] > Data.MAX_LV) supLv[i] = Data.MAX_LV;
                if (supLv[i] < 0) supLv[i] = 0;
            }
            for (int i = 0; i < partLv.length; i++) {
                if (partLv[i] > Data.MAX_LV) partLv[i] = Data.MAX_LV;
                if (partLv[i] < 0) partLv[i] = 0;
            }
            if (weaponLv[0] < 1) weaponLv[0] = 1;
            if (supLv[0] < 1) supLv[0] = 1;
        } catch (Throwable ignored) {
        }
    }

    private void readArr(JSONArray a, int[] dst, int def) {
        if (a == null) return;
        for (int i = 0; i < dst.length; i++) dst[i] = a.optInt(i, def);
    }

    private JSONArray arr(int[] v) {
        JSONArray a = new JSONArray();
        for (int x : v) a.put(x);
        return a;
    }

    public void save() {
        try {
            JSONObject o = new JSONObject();
            o.put("gold", gold);
            o.put("weapon", weapon);
            o.put("sup", sup);
            o.put("maxStage", maxStage);
            o.put("sfx", sfxOn);
            o.put("music", musicOn);
            o.put("kills", totalKills);
            o.put("tgold", totalGold);
            o.put("clears", clears);
            o.put("wl", arr(weaponLv));
            o.put("sl", arr(supLv));
            o.put("pl", arr(partLv));
            o.put("cons", arr(cons));
            o.put("best", arr(bestScore));
            sp.edit().putString("data", o.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    public boolean spend(int amount) {
        if (gold < amount) return false;
        gold -= amount;
        save();
        return true;
    }

    public void earn(int amount) {
        if (amount <= 0) return;
        gold += amount;
        totalGold += amount;
        save();
    }

    public void resetAll() {
        gold = 800;
        weapon = 0;
        weaponLv = new int[]{1, 0, 0};
        sup = 0;
        supLv = new int[]{1, 0, 0};
        partLv = new int[]{0, 0, 0, 0, 0};
        maxStage = 1;
        bestScore = new int[Data.STAGES];
        cons = new int[]{0, 0, 0};
        totalKills = 0;
        totalGold = 0;
        clears = 0;
        save();
    }
}
