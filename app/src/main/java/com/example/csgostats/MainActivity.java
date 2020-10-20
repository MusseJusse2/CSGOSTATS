package com.example.csgostats;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class MainActivity extends AppCompatActivity implements ResultsCallback {

    private TextView mTextViewStats;
    private RequestQueue mQueue;
    private EditText mEditTextInput;
    private Map<String, Integer> statMap = new HashMap<>();
    private Map<String, Integer> achieveMap = new HashMap<>();
    private static String steamID;
    private ProgressBar determinateBarKD;
    private HashMap<String, Integer> lastMap = new HashMap<String, Integer>();
    private HashMap<String, Integer> generalMap = new HashMap<>();
    private HashMap<String, String> profileMap = new HashMap<>();
    private Map<String, Integer> sortedRoundMap = new TreeMap<String, Integer>();
    private Map<String, Integer> sortedWinMap = new TreeMap<String, Integer>();
    private Map<String, Integer> sortedWinRateMap = new TreeMap<String, Integer>();
    private Map<String, Integer> sortedKillMap = new TreeMap<String, Integer>();
    private Map<String, Integer> sortedShotMap = new TreeMap<String, Integer>();
    private Map<String, Integer> sortedHitMap = new TreeMap<String, Integer>();
    private Map<String, Integer> sortedAccuracyMap = new TreeMap<String, Integer>();
    private String avatar;
    PlaceholderFragment taskFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewStats = findViewById(R.id.textViewStats);
        mEditTextInput = findViewById(R.id.editTextInput);
        Button buttonParse = findViewById(R.id.buttonParse);

        mQueue = Volley.newRequestQueue(this);

        buttonParse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTextViewStats.setText("");
                steamID = mEditTextInput.getText().toString();
                if (steamID.contains("https://steamcommunity.com/id/")) { //Check for full profile link
                    steamID = steamID.substring(30, steamID.length()-1); //Extract the custom URL
                }
                if (steamID.matches("^[0-9]*$")) {
                    jsonParse();
                } else {
                    taskFragment = new PlaceholderFragment();
                    getSupportFragmentManager().beginTransaction().add(taskFragment, "MyFragment").commit();
                    taskFragment.startTask();
                    getSupportFragmentManager().beginTransaction().remove(taskFragment);
                }
            }
        });
    }

    public void jsonParse() {
        //76561198308003066 Mine
        //76561198085973818 Joe
        //https://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=B80CA08E579805CE4C2E13AEAB12F358&vanityurl=realmussejusse Get SteamID from custom url

        String url = "https://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?appid=730&key=71B01413ED8FECF15688525E7D776FB9&steamid=" + steamID;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject jsonObject = response.getJSONObject("playerstats");
                            //Get JSONArray of stats
                            JSONArray jsonStats = jsonObject.getJSONArray("stats");
                            for (int i = 0; i < jsonStats.length(); i++) {
                                JSONObject stat = jsonStats.getJSONObject(i);
                                String name = stat.getString("name");
                                int value = stat.getInt("value");
                                statMap.put(name, value);
                            }

                            //Get JSONArray of achievements
                            JSONArray jsonAchievements = jsonObject.getJSONArray("achievements");
                            for (int i = 0; i < jsonAchievements.length(); i++) {
                                JSONObject achievement = jsonAchievements.getJSONObject(i);
                                String name = achievement.getString("name");
                                int achieved = achievement.getInt("achieved");
                                achieveMap.put(name, achieved);
                            }

                            calculateStats();
                            sortMaps();

                            initWeaponsFullKills();
                            //initWeaponsFullShots();
                            //initWeaponsFullAccuracy();
                            initMapsFullWins();
                            //initMapsFullRounds();
                            //initMapsFullWinRate();
                            initWeaponsKills();
                            //initWeaponsShots();
                            //initWeaponsAccuracy();
                            initMapsWins();
                            //initMapsRounds();
                            //initMapsWinRate();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    private void calculateStats() {
        //Calculate KD Ratio
        int totalKills = statMap.get("total_kills");
        int totalDeaths = statMap.get("total_deaths");
        generalMap.put("total_kills", totalKills);
        generalMap.put("total_deaths", totalDeaths);
        float kdRatio = (float) totalKills / totalDeaths;

        //Calculate headshot percentage
        int totalHeadshotKills = statMap.get("total_kills_headshot");
        generalMap.put("total_kills_headshot", totalHeadshotKills);
        float headshotPercentage = (float) totalHeadshotKills / totalKills * 100;

        //Calculate accuracy
        int totalShotsHit = statMap.get("total_shots_hit");
        int totalShotsFired = statMap.get("total_shots_fired");
        generalMap.put("total_shots_hit", totalShotsHit);
        generalMap.put("total_shots_fired", totalShotsFired);
        float accuracy = (float) totalShotsHit / totalShotsFired * 100;

        //Calculate time played in hours
        int timePlayed = statMap.get("total_time_played");
        generalMap.put("total_time_played", timePlayed);
        float hoursPlayed = (float) timePlayed / 60 / 60;
        mTextViewStats.append("\n\nHours Played: " + hoursPlayed);

        //Calculate round win percentage
        int totalRoundWins = statMap.get("total_wins");
        int totalRoundsPlayed = statMap.get("total_rounds_played");
        generalMap.put("total_wins", totalRoundWins);
        generalMap.put("total_rounds_played", totalRoundsPlayed);
        float roundWinPercentage = (float) totalRoundWins / totalRoundsPlayed * 100;

        int totalWins = statMap.get("total_matches_won");
        int totalPlayed = statMap.get("total_matches_played");
        generalMap.put("total_matches_won", totalWins);
        generalMap.put("total_matches_played", totalPlayed);
        float winPercentage = (float) totalWins / totalPlayed * 100;

        int totalMVP = statMap.get("total_mvps");
        generalMap.put("total_mvps", totalMVP);
        mTextViewStats.append("\n\nMVPS: " + totalMVP);

        int totalDamage = statMap.get("total_damage_done");
        generalMap.put("total_damage_done", totalDamage);
        float adr = (float) totalDamage / totalRoundsPlayed;
        mTextViewStats.append("\n\nADR: " + adr);

        float kpr = (float) totalKills / totalRoundsPlayed;
        mTextViewStats.append("\n\nKPR: " + kpr);

        float dpr = (float) totalDeaths / totalRoundsPlayed;
        mTextViewStats.append("\n\nDPR: " + dpr);

        //Calculate achievements completed
        int totalAchieved = achieveMap.size();
        generalMap.put("total_achieved", totalAchieved);
        mTextViewStats.append("\n\nTotal Achieved: " + totalAchieved + "\n");

        //Calculate achievement completion percentage
        int totalAchievements = 167;
        float achievementCompletion = (float) totalAchieved / totalAchievements * 100;

        //Progress bar showing KD
        TextView textViewKD = findViewById(R.id.textViewKD);
        CircularProgressBar circularProgressBarKD = (CircularProgressBar) findViewById(R.id.progressBarKD);
        int animationDuration = 2500; // 2500ms = 2,5s
        float progressKD = (float) totalKills / (totalDeaths + totalKills) * 100;
        circularProgressBarKD.setProgressWithAnimation(progressKD, animationDuration);
        kdRatio = ((int) ((kdRatio + 0.005f) * 100)) / 100f; //Round to 2DP
        textViewKD.setText("KD: " + kdRatio);
        circularProgressBarKD.setVisibility(View.VISIBLE);
        textViewKD.setVisibility(View.VISIBLE);

        //Progress bar showing HS
        TextView textViewHS = findViewById(R.id.textViewHS);
        CircularProgressBar circularProgressBarHS = (CircularProgressBar) findViewById(R.id.progressBarHS);
        circularProgressBarHS.setProgressWithAnimation(headshotPercentage, animationDuration);
        headshotPercentage = ((int) ((headshotPercentage + 0.005f) * 100)) / 100f; //Round to 2DP
        textViewHS.setText("HS: " + Math.round(headshotPercentage) + "%");
        circularProgressBarHS.setVisibility(View.VISIBLE);
        textViewHS.setVisibility(View.VISIBLE);

        //Progress bar showing Accuracy
        TextView textViewAccuracy = findViewById(R.id.textViewAccuracy);
        CircularProgressBar circularProgressBarAccuracy = (CircularProgressBar) findViewById(R.id.progressBarAccuracy);
        circularProgressBarAccuracy.setProgressWithAnimation(accuracy, animationDuration);
        accuracy = ((int) ((accuracy + 0.005f) * 100)) / 100f; //Round to 2DP
        textViewAccuracy.setText("Accuracy: " + Math.round(accuracy) + "%");
        circularProgressBarAccuracy.setVisibility(View.VISIBLE);
        textViewAccuracy.setVisibility(View.VISIBLE);

        //Progress bar showing Round Win Percentage
        TextView textViewRoundWinRate = findViewById(R.id.textViewRoundWinRate);
        CircularProgressBar circularProgressBarRoundWin = (CircularProgressBar) findViewById(R.id.progressBarRoundWin);
        circularProgressBarRoundWin.setProgressWithAnimation(roundWinPercentage, animationDuration);
        roundWinPercentage = ((int) ((roundWinPercentage + 0.005f) * 100)) / 100f; //Round to 2DP
        textViewRoundWinRate.setText("Round: " + Math.round(roundWinPercentage) + "%");
        circularProgressBarRoundWin.setVisibility(View.VISIBLE);
        textViewRoundWinRate.setVisibility(View.VISIBLE);

        //Progress bar showing Win Percentage
        TextView textViewWinRate = findViewById(R.id.textViewWinRate);
        CircularProgressBar circularProgressBarWinRate = (CircularProgressBar) findViewById(R.id.progressBarWinRate);
        circularProgressBarWinRate.setProgressWithAnimation(winPercentage, animationDuration);
        winPercentage = ((int) ((winPercentage + 0.005f) * 100)) / 100f; //Round to 2DP
        textViewWinRate.setText("Win: " + Math.round(winPercentage) + "%");
        circularProgressBarWinRate.setVisibility(View.VISIBLE);
        textViewWinRate.setVisibility(View.VISIBLE);

        //Progress bar showing Achievement Completion
        TextView textViewAchievements = findViewById(R.id.textViewAchievements);
        CircularProgressBar circularProgressBarAchievements = (CircularProgressBar) findViewById(R.id.progressBarAchievements);
        circularProgressBarAchievements.setProgressWithAnimation(achievementCompletion, animationDuration);
        achievementCompletion = ((int) ((achievementCompletion + 0.005f) * 100)) / 100f; //Round to 2DP
        textViewAchievements.setText("Achieved: " + Math.round(achievementCompletion) + "%");
        circularProgressBarAchievements.setVisibility(View.VISIBLE);
        textViewAchievements.setVisibility(View.VISIBLE);
    }

    private void sortMaps() {
        for (String key : statMap.keySet()) {
            if (key.contains("last_match") && !key.contains("gg_contribution_score") && !key.contains("dominations") && !key.contains("revenges") && !key.contains("max_players")) {
                lastMap.put(key, statMap.get(key));
            } else if (key.contains("total_rounds_map_")) {
                sortedRoundMap.put(key, statMap.get(key));
            } else if (key.contains("total_wins_map_") && !key.contains("total_wins_map_de_house")) {
                sortedWinMap.put(key, statMap.get(key));
            } else if (key.contains("total_kills_") && !key.contains("headshot") && !key.contains("zoomed") && !key.contains("knife_fight") && !key.contains("enemy")) {
                sortedKillMap.put(key, statMap.get(key));
            } else if (key.contains("total_shots_") && !key.contains("fired") && !key.contains("hit")) {
                sortedShotMap.put(key, statMap.get(key));
            } else if (key.contains("total_hits_")) {
                sortedHitMap.put(key, statMap.get(key));
            }
        }

        //Sorted win rate map
        for (String keyRound : sortedRoundMap.keySet()) {
            for (String keyWin : sortedWinMap.keySet()) {
                if (keyRound.substring(17).contains(keyWin.substring(15))) {
                    float winRate = (float) sortedWinMap.get(keyWin) / sortedRoundMap.get(keyRound) * 100;
                    sortedWinRateMap.put("win_rate_" + keyRound.substring(17), Math.round(winRate));
                }
            }
        }

        //Sorted Accuracy Map
        for (String keyShot : sortedShotMap.keySet()) {
            for (String keyHit : sortedHitMap.keySet()) {
                if (keyShot.substring(12).contains(keyHit.substring(11))) {
                    float weaponAccuracy = (float) sortedHitMap.get(keyHit) / sortedShotMap.get(keyShot) * 100;
                    //Taser doesn't have a hits value so must use kills instead of hits (this is fine since its a one shot kill anyway so a hit = a kill)
                    float taserAccuracy = (float) sortedKillMap.get("total_kills_taser") / sortedShotMap.get("total_shots_taser") * 100;
                    sortedAccuracyMap.put("accuracy_" + keyHit.substring(11), Math.round(weaponAccuracy));
                    sortedAccuracyMap.put("accuracy_taser", Math.round(taserAccuracy));
                }
            }
        }

        //No accuracy for molotov and hegrenade and knife

        //mTextViewStats.append(String.valueOf(lastMap));
        sortedRoundMap = sortByValue(sortedRoundMap);
        //mTextViewStats.append(String.valueOf(sortByValue(sortedRoundMap)));
        sortedWinMap = sortByValue(sortedWinMap);
        //mTextViewStats.append(String.valueOf(sortByValue(sortedWinMap)));
        sortedWinRateMap = sortByValue(sortedWinRateMap);
        //mTextViewStats.append(String.valueOf(sortByValue(sortedWinRateMap)));
        sortedKillMap = sortByValue(sortedKillMap);
        //mTextViewStats.append(String.valueOf(sortByValue(sortedKillMap)));
        sortedShotMap = sortByValue(sortedShotMap);
        //mTextViewStats.append(String.valueOf(sortByValue(sortedShotMap)));
        sortedHitMap = sortByValue(sortedHitMap);
        //mTextViewStats.append(String.valueOf(sortByValue(sortedHitMap)));
        //mTextViewStats.append(String.valueOf(sortByValue(sortedAccuracyMap)) + "\n");
    }

    private void getSteamIDFromURL() {
        String url = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=71B01413ED8FECF15688525E7D776FB9&vanityurl=" + mEditTextInput.getText();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject jsonID = response.getJSONObject("response");
                            steamID = jsonID.getString("steamid");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    public static Map<String, Integer> sortByValue(Map<String, Integer> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer>> list =
                new LinkedList<Map.Entry<String, Integer>>(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        //Reverses the list so highest number is first
        Collections.reverse(list);

        // put data from sorted list to hashmap
        Map<String, Integer> temp = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public void createMapFull() {
        TableLayout stk = (TableLayout) findViewById(R.id.table_maps_full);
        stk.removeAllViews();
        TableRow tbrow0 = new TableRow(this);
        TextView tv0 = new TextView(this);
        tv0.setText("");
        tv0.setTextColor(Color.WHITE);
        tv0.setGravity(Gravity.CENTER);
        tbrow0.addView(tv0);
        TextView tv1 = new TextView(this);
        tv1.setText("");
        tv1.setTextColor(Color.WHITE);
        tv1.setGravity(Gravity.CENTER);
        tbrow0.addView(tv1);
        TextView tv2 = new TextView(this);
        tv2.setText("Wins");
        tv2.setTextColor(Color.WHITE);
        tv2.setGravity(Gravity.CENTER);
        tbrow0.addView(tv2);
        TextView tv3 = new TextView(this);
        tv3.setText("Rounds");
        tv3.setTextColor(Color.WHITE);
        tbrow0.addView(tv3);
        TextView tv4 = new TextView(this);
        tv4.setText("Win Rate");
        tv4.setTextColor(Color.WHITE);
        tbrow0.addView(tv4);
        stk.addView(tbrow0);
    }

    public void createMap(String sortBy) {
        TableLayout stk = (TableLayout) findViewById(R.id.table_maps);
        stk.removeAllViews();
        TableRow tbrow0 = new TableRow(this);
        TextView tv0 = new TextView(this);
        tv0.setText("");
        tv0.setTextColor(Color.WHITE);
        tv0.setGravity(Gravity.CENTER);
        tbrow0.addView(tv0);
        TextView tv1 = new TextView(this);
        tv1.setText(sortBy);
        tv1.setTextColor(Color.WHITE);
        tv1.setGravity(Gravity.CENTER);
        tbrow0.addView(tv1);
        stk.addView(tbrow0);
    }

    public void createWeaponFull() {
        TableLayout stk = (TableLayout) findViewById(R.id.table_weapons_full);
        stk.removeAllViews();
        TableRow tbrow0 = new TableRow(this);
        TextView tv0 = new TextView(this);
        tv0.setText("");
        tv0.setTextColor(Color.WHITE);
        tv0.setGravity(Gravity.CENTER);
        tbrow0.addView(tv0);
        TextView tv1 = new TextView(this);
        tv1.setText("");
        tv1.setTextColor(Color.WHITE);
        tv1.setGravity(Gravity.CENTER);
        tbrow0.addView(tv1);
        //tv1.getLayoutParams().width = 30;
        TextView tv2 = new TextView(this);
        tv2.setText("Kills");
        tv2.setTextColor(Color.WHITE);
        tv2.setGravity(Gravity.CENTER);
        tbrow0.addView(tv2);
        //TableLayout.LayoutParams rowParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT, 1f);
        TextView tv3 = new TextView(this);
        tv3.setText("Shots");
        tv3.setTextColor(Color.WHITE);
        tbrow0.addView(tv3);
        TextView tv4 = new TextView(this);
        tv4.setText("Accuracy");
        tv4.setTextColor(Color.WHITE);
        tbrow0.addView(tv4);
        stk.addView(tbrow0);
    }

    public void createWeapon(String sortBy) {
        TableLayout stk = (TableLayout) findViewById(R.id.table_weapons);
        stk.removeAllViews();
        TableRow tbrow0 = new TableRow(this);
        TextView tv0 = new TextView(this);
        tv0.setText("");
        tv0.setTextColor(Color.WHITE);
        tv0.setGravity(Gravity.CENTER);
        tbrow0.addView(tv0);
        TextView tv1 = new TextView(this);
        tv1.setText(sortBy);
        tv1.setTextColor(Color.WHITE);
        tv1.setGravity(Gravity.CENTER);
        tbrow0.addView(tv1);
        stk.addView(tbrow0);
    }

    public void initMapsFullWins() {
        Resources resources = getResources();
        int resourceId = 0;
        createMapFull();
        TableLayout stk = (TableLayout) findViewById(R.id.table_maps_full);
        //Sorted by Wins
        for (String key : sortedWinMap.keySet()) {
            TableRow tbrow = new TableRow(this);
            resourceId = resources.getIdentifier("map_icon_" + key.substring(15), "drawable", getPackageName());
            ImageView iv0 = new ImageView(this);
            if (key.substring(15).contains("de_inferno")) {
                resourceId = resources.getIdentifier("map_icon_de_inferno_png", "drawable", getPackageName());
            } else if (key.substring(15).contains("de_shorttrain")) {
                resourceId = resources.getIdentifier("map_icon_none_png", "drawable", getPackageName());
            }
            iv0.setImageResource(resourceId);
            tbrow.addView(iv0);
            iv0.getLayoutParams().height = 200;
            iv0.getLayoutParams().width = 200;
            TextView t1v = new TextView(this);
            t1v.setText(key.substring(18).toUpperCase());
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            TextView t2v = new TextView(this);
            t2v.setText(String.valueOf(sortedWinMap.get(key)));
            t2v.setTextColor(Color.WHITE);
            t2v.setGravity(Gravity.CENTER);
            tbrow.addView(t2v);
            TextView t3v = new TextView(this);
            t3v.setText(String.valueOf(sortedRoundMap.get("total_rounds_map_" + key.substring(15))));
            t3v.setTextColor(Color.WHITE);
            t3v.setGravity(Gravity.CENTER);
            tbrow.addView(t3v);
            TextView t4v = new TextView(this);
            t4v.setText(String.valueOf(sortedWinRateMap.get("win_rate_" + key.substring(15))) + "%");
            t4v.setTextColor(Color.WHITE);
            t4v.setGravity(Gravity.CENTER);
            tbrow.addView(t4v);
            stk.addView(tbrow);
        }
    }

    public void initMapsFullRounds() {
        Resources resources = getResources();
        int resourceId = 0;
        createMapFull();
        TableLayout stk = (TableLayout) findViewById(R.id.table_maps_full);
        //Sorted by Rounds
        for (String key : sortedRoundMap.keySet()) {
            TableRow tbrow = new TableRow(this);
            resourceId = resources.getIdentifier("map_icon_" + key.substring(17), "drawable", getPackageName());
            ImageView iv0 = new ImageView(this);
            if (key.substring(15).contains("de_inferno")) {
                resourceId = resources.getIdentifier("map_icon_de_inferno_png", "drawable", getPackageName());
            } else if (key.substring(15).contains("de_shorttrain")) {
                resourceId = resources.getIdentifier("map_icon_none_png", "drawable", getPackageName());
            }
            iv0.setImageResource(resourceId);
            tbrow.addView(iv0);
            iv0.getLayoutParams().height = 200;
            iv0.getLayoutParams().width = 200;
            TextView t1v = new TextView(this);
            t1v.setText(key.substring(20).toUpperCase());
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            TextView t2v = new TextView(this);
            t2v.setText(String.valueOf(sortedWinMap.get("total_wins_map_" + key.substring(17))));
            t2v.setTextColor(Color.WHITE);
            t2v.setGravity(Gravity.CENTER);
            tbrow.addView(t2v);
            TextView t3v = new TextView(this);
            t3v.setText(String.valueOf(sortedRoundMap.get(key)));
            t3v.setTextColor(Color.WHITE);
            t3v.setGravity(Gravity.CENTER);
            tbrow.addView(t3v);
            TextView t4v = new TextView(this);
            t4v.setText(String.valueOf(sortedWinRateMap.get("win_rate_" + key.substring(17))) + "%");
            t4v.setTextColor(Color.WHITE);
            t4v.setGravity(Gravity.CENTER);
            tbrow.addView(t4v);
            stk.addView(tbrow);
        }
    }

    public void initMapsFullWinRate() {
        Resources resources = getResources();
        int resourceId = 0;
        createMapFull();
        TableLayout stk = (TableLayout) findViewById(R.id.table_maps_full);
        //Sorted by Win Rate
        for (String key : sortedWinRateMap.keySet()) {
            TableRow tbrow = new TableRow(this);
            resourceId = resources.getIdentifier("map_icon_" + key.substring(9), "drawable", getPackageName());
            ImageView iv0 = new ImageView(this);
            if (key.substring(9).contains("de_inferno")) {
                resourceId = resources.getIdentifier("map_icon_de_inferno_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("de_shorttrain")) {
                resourceId = resources.getIdentifier("map_icon_none_png", "drawable", getPackageName());
            }
            iv0.setImageResource(resourceId);
            tbrow.addView(iv0);
            iv0.getLayoutParams().height = 200;
            iv0.getLayoutParams().width = 200;
            TextView t1v = new TextView(this);
            t1v.setText(key.substring(12).toUpperCase());
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            TextView t2v = new TextView(this);
            t2v.setText(String.valueOf(sortedWinMap.get("total_wins_map_" + key.substring(9))));
            t2v.setTextColor(Color.WHITE);
            t2v.setGravity(Gravity.CENTER);
            tbrow.addView(t2v);
            TextView t3v = new TextView(this);
            t3v.setText(String.valueOf(sortedRoundMap.get("total_rounds_map_" + key.substring(9))));
            t3v.setTextColor(Color.WHITE);
            t3v.setGravity(Gravity.CENTER);
            tbrow.addView(t3v);
            TextView t4v = new TextView(this);
            t4v.setText(String.valueOf(sortedWinRateMap.get(key) + "%"));
            t4v.setTextColor(Color.WHITE);
            t4v.setGravity(Gravity.CENTER);
            tbrow.addView(t4v);
            stk.addView(tbrow);
        }
    }

    public void initMapsWins() {
        Resources resources = getResources();
        int resourceId = 0;
        int count = 0;
        createMap("Wins");
        TableLayout stk = (TableLayout) findViewById(R.id.table_maps);
        //Sorted by Wins
        for (String key : sortedWinMap.keySet()) {
            if (count == 5) {
                break;
            } else {
                TableRow tbrow = new TableRow(this);
                resourceId = resources.getIdentifier("map_icon_" + key.substring(15), "drawable", getPackageName());
                ImageView iv0 = new ImageView(this);
                if (key.substring(15).contains("de_inferno")) {
                    resourceId = resources.getIdentifier("map_icon_de_inferno_png", "drawable", getPackageName());
                } else if (key.substring(15).contains("de_shorttrain")) {
                    resourceId = resources.getIdentifier("map_icon_none_png", "drawable", getPackageName());
                }
                iv0.setImageResource(resourceId);
                tbrow.addView(iv0);
                iv0.getLayoutParams().height = 200;
                iv0.getLayoutParams().width = 200;
                TextView t1v = new TextView(this);
                t1v.setText(String.valueOf(sortedWinMap.get(key)));
                t1v.setTextColor(Color.WHITE);
                t1v.setGravity(Gravity.CENTER);
                tbrow.addView(t1v);
                stk.addView(tbrow);
                count++;
            }
        }
        stk.setVisibility(View.VISIBLE);
    }

    public void initMapsRounds() {
        Resources resources = getResources();
        int resourceId = 0;
        int count = 0;
        createMap("Rounds");
        TableLayout stk = (TableLayout) findViewById(R.id.table_maps);
        //Sorted by Rounds
        for (String key : sortedRoundMap.keySet()) {
            if (count == 5) {
                break;
            } else {
                TableRow tbrow = new TableRow(this);
                resourceId = resources.getIdentifier("map_icon_" + key.substring(17), "drawable", getPackageName());
                ImageView iv0 = new ImageView(this);
                if (key.substring(17).contains("de_inferno")) {
                    resourceId = resources.getIdentifier("map_icon_de_inferno_png", "drawable", getPackageName());
                } else if (key.substring(17).contains("de_shorttrain")) {
                    resourceId = resources.getIdentifier("map_icon_none_png", "drawable", getPackageName());
                }
                iv0.setImageResource(resourceId);
                tbrow.addView(iv0);
                iv0.getLayoutParams().height = 200;
                iv0.getLayoutParams().width = 200;
                TextView t1v = new TextView(this);
                t1v.setText(String.valueOf(sortedRoundMap.get(key)));
                t1v.setTextColor(Color.WHITE);
                t1v.setGravity(Gravity.CENTER);
                tbrow.addView(t1v);
                stk.addView(tbrow);
                count++;
            }
        }
    }

    public void initMapsWinRate() {
        Resources resources = getResources();
        int resourceId = 0;
        int count = 0;
        createMap("Win Rate");
        TableLayout stk = (TableLayout) findViewById(R.id.table_maps);

        //Sorted by Win rate
        for (String key : sortedWinRateMap.keySet()) {
            if (count == 5) {
                break;
            } else {
                TableRow tbrow = new TableRow(this);
                resourceId = resources.getIdentifier("map_icon_" + key.substring(9), "drawable", getPackageName());
                ImageView iv0 = new ImageView(this);
                if (key.substring(9).contains("de_inferno")) {
                    resourceId = resources.getIdentifier("map_icon_de_inferno_png", "drawable", getPackageName());
                } else if (key.substring(9).contains("de_shorttrain")) {
                    resourceId = resources.getIdentifier("map_icon_none_png", "drawable", getPackageName());
                }
                iv0.setImageResource(resourceId);
                tbrow.addView(iv0);
                iv0.getLayoutParams().height = 200;
                iv0.getLayoutParams().width = 200;
                TextView t1v = new TextView(this);
                t1v.setText(String.valueOf(sortedWinRateMap.get(key)) + "%");
                t1v.setTextColor(Color.WHITE);
                t1v.setGravity(Gravity.CENTER);
                tbrow.addView(t1v);
                stk.addView(tbrow);
                count++;
            }
        }
    }

    public void initWeaponsFullKills() {
        Resources resources = getResources();
        int resourceId = 0;
        createWeaponFull();
        TableLayout stk = (TableLayout) findViewById(R.id.table_weapons_full);
        //Sorted by kills
        for (String key : sortedKillMap.keySet()) {
            TableRow tbrow = new TableRow(this);
            resourceId = resources.getIdentifier(key.substring(12), "drawable", getPackageName());
            ImageView iv0 = new ImageView(this);
            if (key.substring(12).contains("hkp2000")) {
                resourceId = resources.getIdentifier("hkp2000_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("mp7")) {
                resourceId = resources.getIdentifier("mp7_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("negev")) {
                resourceId = resources.getIdentifier("negev_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("ssg08")) {
                resourceId = resources.getIdentifier("ssg08_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("scar20")) {
                resourceId = resources.getIdentifier("scar20_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("hegrenade")) {
                resourceId = resources.getIdentifier("hegrenade_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("bizon")) {
                resourceId = resources.getIdentifier("bizon_png", "drawable", getPackageName());
            }
            iv0.setImageResource(resourceId);
            tbrow.addView(iv0);
            TextView t1v = new TextView(this);
            t1v.setText(key.substring(12).toUpperCase());
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            TextView t2v = new TextView(this);
            t2v.setText(String.valueOf(sortedKillMap.get(key)));
            t2v.setTextColor(Color.WHITE);
            t2v.setGravity(Gravity.LEFT);
            tbrow.addView(t2v);
            TextView t3v = new TextView(this);
            t3v.setText(String.valueOf(sortedShotMap.get("total_shots_" + key.substring(12))));
            t3v.setTextColor(Color.WHITE);
            t3v.setGravity(Gravity.LEFT);
            tbrow.addView(t3v);
            TextView t4v = new TextView(this);
            t4v.setText(String.valueOf(sortedAccuracyMap.get("accuracy_" + key.substring(12))) + "%");
            t4v.setTextColor(Color.WHITE);
            t4v.setGravity(Gravity.LEFT);
            tbrow.addView(t4v);
            stk.addView(tbrow);
        }
    }

    public void initWeaponsFullShots() {
        Resources resources = getResources();
        int resourceId = 0;
        createWeaponFull();
        TableLayout stk = (TableLayout) findViewById(R.id.table_weapons_full);
        //Sorted by kills
        for (String key : sortedShotMap.keySet()) {
            TableRow tbrow = new TableRow(this);
            resourceId = resources.getIdentifier(key.substring(12), "drawable", getPackageName());
            ImageView iv0 = new ImageView(this);
            if (key.substring(12).contains("hkp2000")) {
                resourceId = resources.getIdentifier("hkp2000_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("mp7")) {
                resourceId = resources.getIdentifier("mp7_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("negev")) {
                resourceId = resources.getIdentifier("negev_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("ssg08")) {
                resourceId = resources.getIdentifier("ssg08_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("scar20")) {
                resourceId = resources.getIdentifier("scar20_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("hegrenade")) {
                resourceId = resources.getIdentifier("hegrenade_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("bizon")) {
                resourceId = resources.getIdentifier("bizon_png", "drawable", getPackageName());
            }
            iv0.setImageResource(resourceId);
            tbrow.addView(iv0);
            TextView t1v = new TextView(this);
            t1v.setText(key.substring(12).toUpperCase());
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            TextView t2v = new TextView(this);
            t2v.setText(String.valueOf(sortedKillMap.get("total_kills_" + key.substring(12))));
            t2v.setTextColor(Color.WHITE);
            t2v.setGravity(Gravity.CENTER);
            tbrow.addView(t2v);
            TextView t3v = new TextView(this);
            t3v.setText(String.valueOf(sortedShotMap.get(key)));
            t3v.setTextColor(Color.WHITE);
            t3v.setGravity(Gravity.CENTER);
            tbrow.addView(t3v);
            TextView t4v = new TextView(this);
            t4v.setText(String.valueOf(sortedAccuracyMap.get("accuracy_" + key.substring(12))) + "%");
            t4v.setTextColor(Color.WHITE);
            t4v.setGravity(Gravity.CENTER);
            tbrow.addView(t4v);
            stk.addView(tbrow);
        }
        stk.setVisibility(View.VISIBLE);
    }

    public void initWeaponsFullAccuracy() {
        Resources resources = getResources();
        int resourceId = 0;
        createWeaponFull();
        TableLayout stk = (TableLayout) findViewById(R.id.table_weapons_full);
        //Sorted by kills
        for (String key : sortedAccuracyMap.keySet()) {
            TableRow tbrow = new TableRow(this);
            resourceId = resources.getIdentifier(key.substring(9), "drawable", getPackageName());
            ImageView iv0 = new ImageView(this);
            if (key.substring(9).contains("hkp2000")) {
                resourceId = resources.getIdentifier("hkp2000_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("mp7")) {
                resourceId = resources.getIdentifier("mp7_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("negev")) {
                resourceId = resources.getIdentifier("negev_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("ssg08")) {
                resourceId = resources.getIdentifier("ssg08_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("scar20")) {
                resourceId = resources.getIdentifier("scar20_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("hegrenade")) {
                resourceId = resources.getIdentifier("hegrenade_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("bizon")) {
                resourceId = resources.getIdentifier("bizon_png", "drawable", getPackageName());
            }
            iv0.setImageResource(resourceId);
            tbrow.addView(iv0);
            TextView t1v = new TextView(this);
            t1v.setText(key.substring(9).toUpperCase());
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            TextView t2v = new TextView(this);
            t2v.setText(String.valueOf(sortedKillMap.get("total_kills_" + key.substring(9))));
            t2v.setTextColor(Color.WHITE);
            t2v.setGravity(Gravity.CENTER);
            tbrow.addView(t2v);
            TextView t3v = new TextView(this);
            t3v.setText(String.valueOf(sortedShotMap.get("total_shots_" + key.substring(9))));
            t3v.setTextColor(Color.WHITE);
            t3v.setGravity(Gravity.CENTER);
            tbrow.addView(t3v);
            TextView t4v = new TextView(this);
            t4v.setText(String.valueOf(sortedAccuracyMap.get(key) + "%"));
            t4v.setTextColor(Color.WHITE);
            t4v.setGravity(Gravity.CENTER);
            tbrow.addView(t4v);
            stk.addView(tbrow);
        }
    }

    public void initWeaponsKills() {
        Resources resources = getResources();
        int resourceId = 0;
        int count = 0;
        createWeapon("Kills");
        TableLayout stk = (TableLayout) findViewById(R.id.table_weapons);
        //Sorted by kills
        for (String key : sortedKillMap.keySet()) {
            if (count == 5) {
                break;
            }
            TableRow tbrow = new TableRow(this);
            resourceId = resources.getIdentifier(key.substring(12), "drawable", getPackageName());
            ImageView iv0 = new ImageView(this);
            if (key.substring(12).contains("hkp2000")) {
                resourceId = resources.getIdentifier("hkp2000_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("mp7")) {
                resourceId = resources.getIdentifier("mp7_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("negev")) {
                resourceId = resources.getIdentifier("negev_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("ssg08")) {
                resourceId = resources.getIdentifier("ssg08_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("scar20")) {
                resourceId = resources.getIdentifier("scar20_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("hegrenade")) {
                resourceId = resources.getIdentifier("hegrenade_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("bizon")) {
                resourceId = resources.getIdentifier("bizon_png", "drawable", getPackageName());
            }
            iv0.setImageResource(resourceId);
            tbrow.addView(iv0);
            TextView t1v = new TextView(this);
            t1v.setText(String.valueOf(sortedKillMap.get(key)));
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            stk.addView(tbrow);
            count++;
        }
    }

    public void initWeaponsShots() {
        Resources resources = getResources();
        int resourceId = 0;
        int count = 0;
        createWeapon("Shots");
        TableLayout stk = (TableLayout) findViewById(R.id.table_weapons);
        //Sorted by shots
        for (String key : sortedShotMap.keySet()) {
            if (count == 5) {
                break;
            }
            TableRow tbrow = new TableRow(this);
            resourceId = resources.getIdentifier(key.substring(12), "drawable", getPackageName());
            ImageView iv0 = new ImageView(this);
            if (key.substring(12).contains("hkp2000")) {
                resourceId = resources.getIdentifier("hkp2000_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("mp7")) {
                resourceId = resources.getIdentifier("mp7_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("negev")) {
                resourceId = resources.getIdentifier("negev_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("ssg08")) {
                resourceId = resources.getIdentifier("ssg08_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("scar20")) {
                resourceId = resources.getIdentifier("scar20_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("hegrenade")) {
                resourceId = resources.getIdentifier("hegrenade_png", "drawable", getPackageName());
            } else if (key.substring(12).contains("bizon")) {
                resourceId = resources.getIdentifier("bizon_png", "drawable", getPackageName());
            }
            iv0.setImageResource(resourceId);
            tbrow.addView(iv0);
            TextView t1v = new TextView(this);
            t1v.setText(String.valueOf(sortedShotMap.get(key)));
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            stk.addView(tbrow);
            count++;
        }
    }

    public void initWeaponsAccuracy() {
        Resources resources = getResources();
        int resourceId = 0;
        int count = 0;
        createWeapon("Accuracy");
        TableLayout stk = (TableLayout) findViewById(R.id.table_weapons);

        //Sorted by shots
        for (String key : sortedAccuracyMap.keySet()) {
            if (count == 5) {
                break;
            }
            TableRow tbrow = new TableRow(this);
            resourceId = resources.getIdentifier(key.substring(9), "drawable", getPackageName());
            ImageView iv0 = new ImageView(this);
            if (key.substring(9).contains("hkp2000")) {
                resourceId = resources.getIdentifier("hkp2000_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("mp7")) {
                resourceId = resources.getIdentifier("mp7_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("negev")) {
                resourceId = resources.getIdentifier("negev_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("ssg08")) {
                resourceId = resources.getIdentifier("ssg08_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("scar20")) {
                resourceId = resources.getIdentifier("scar20_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("hegrenade")) {
                resourceId = resources.getIdentifier("hegrenade_png", "drawable", getPackageName());
            } else if (key.substring(9).contains("bizon")) {
                resourceId = resources.getIdentifier("bizon_png", "drawable", getPackageName());
            }
            iv0.setImageResource(resourceId);
            tbrow.addView(iv0);
            TextView t1v = new TextView(this);
            t1v.setText(String.valueOf(sortedAccuracyMap.get(key)) + "%");
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            stk.addView(tbrow);
            count++;
        }
    }

    @Override
    public void onPreExcecute() {

    }

    @Override
    public void onPostExecute(HashMap<String, String> infoMap) {
        ImageView ivAvatar = findViewById(R.id.ivAvatar);
        steamID = infoMap.get("steamID64");
        TextView tvName = findViewById(R.id.textViewName);
        tvName.setText(infoMap.get("steamID"));
        avatar = infoMap.get("avatarFull");
        Picasso.get().load(avatar).into(ivAvatar);
        ivAvatar.getLayoutParams().height = 250;
        ivAvatar.getLayoutParams().width = 250;
        jsonParse();
        profileMap = (HashMap<String, String>) infoMap.clone();
    }

    public static class PlaceholderFragment extends Fragment {
        XMLTask xmlTask;
        ResultsCallback callback;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            callback = (ResultsCallback) activity;
            if (xmlTask != null) {
                xmlTask.onAttach(callback);
            }
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setRetainInstance(true);
        }

        public void startTask() {
            if (xmlTask != null) {
                xmlTask.cancel(true);
            } else {
                xmlTask = new XMLTask(callback);
                xmlTask.execute();
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            callback = null;
            if (xmlTask != null) {
                xmlTask.onDetach();
            }
        }
    }

    public static class XMLTask extends AsyncTask<Void, Void, HashMap<String, String>> {

        ResultsCallback callback = null;

        public XMLTask(ResultsCallback callback) {
            this.callback = callback;
        }

        public void onAttach(ResultsCallback callback) {
            this.callback = callback;
        }

        public void onDetach() {
            callback = null;
        }

        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onPreExcecute();
            }
        }

        @Override
        protected HashMap<String, String> doInBackground(Void... params) {
            HashMap<String, String> infoMap = new HashMap<>();
            try {
                URL url = new URL("https://steamcommunity.com/id/" + steamID + "?xml=1");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                InputStream inputStream = connection.getInputStream();
                infoMap = processXML(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return infoMap;
        }

        public HashMap<String, String> processXML(InputStream inputStream) throws Exception {
            HashMap<String, String> infoMap = new HashMap<>();
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document xml = documentBuilder.parse(inputStream);

            Element rootElement = xml.getDocumentElement();
            NodeList itemList = rootElement.getElementsByTagName("steamID64");
            Node currentItem = null;
            currentItem = itemList.item(0);
            infoMap.put(currentItem.getNodeName(), currentItem.getTextContent());

            itemList = rootElement.getElementsByTagName("steamID");
            currentItem = itemList.item(0);
            infoMap.put(currentItem.getNodeName(), currentItem.getTextContent());

            itemList = rootElement.getElementsByTagName("avatarFull");
            currentItem = itemList.item(0);
            infoMap.put(currentItem.getNodeName(), currentItem.getTextContent());
            return infoMap;
        }

        @Override
        protected void onPostExecute(HashMap<String, String> infoMap) {
            if (callback != null) {
                callback.onPostExecute(infoMap);
            }
        }
    }
}

interface ResultsCallback {
    public void onPreExcecute();
    public void onPostExecute(HashMap<String, String> infoMap);
}
