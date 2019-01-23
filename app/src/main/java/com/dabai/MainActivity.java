package com.dabai;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //变量

    //总控状态
    boolean listen_all = false;
    boolean light = false;
    static int DB;
    boolean flash = false;
    int win_light = 0;

    LightSensorManager lsm;
    //控件状态变量
    int pro_num1 = 60, pro_num2 = 100, pro_num3 = 10, pro_num4 = 1;
    boolean sw_ch1, sw_ch2, sw_ch3, sw_ch4;


    //控件对象
    TextView lay1_1, lay1_2, info;
    SeekBar lay2_1, lay2_2, lay2_3;
    SeekBar lay3_2;

    Switch lay3_1;
    Switch lay4_1, lay4_2, lay4_3;


    private CameraManager manager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //不要忘了在清单申请,问题：权限申请时间不对
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            }, 1); // 动态申请读取权限
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //如果当前平台版本大于23平台
            if (!Settings.System.canWrite(this)) {
                //如果没有修改系统的权限这请求修改系统的权限

                new AlertDialog.Builder(this)
                        .setTitle("提示")
                        .setMessage("必须允许\"修改系统设置\"，才能使用部分功能！")
                        .setPositiveButton("去设置",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                        intent.setData(Uri.parse("package:" + getPackageName()));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivityForResult(intent, 0);
                                    }
                                })
                        .setNeutralButton("取消", null)
                        .show();


            } else {
                //有了权限，你要做什么呢？具体的动作

            }
        }


        SharedPreferences sp = this.getSharedPreferences("data", 0);

        SharedPreferences.Editor editor = sp.edit();

        win_light = getSystemBrightness();

        //showText("" + sp.getBoolean("first", false));

        if (sp.getBoolean("first", false) == true) {

            init_val();
            init();
        } else {
            editor.putBoolean("first", true);
            editor.commit();
        }


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {

                if (light) {

                    light_off();
                } else {

                    light_on();
                }
            }
        });
    }


    @TargetApi(Build.VERSION_CODES.M)
    public void light_on() {
        try {
            manager.setTorchMode("0", true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        light = true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void light_off() {
        try {
            manager.setTorchMode("0", false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        light = false;
    }


    private Handler handler = new Handler();
    private Runnable task = new Runnable() {

        @TargetApi(Build.VERSION_CODES.M)
        public void run() {
            changeAppBrightness(win_light);

            handler.postDelayed(this, 100);//设置循环时间，此处是5秒
            //满足条件 (光线低，声音够大，灯没亮，总控监听)

            if (listen_all == true && light == false) {
                info.setBackgroundColor(Color.parseColor("#A5D6A7"));
                if (lsm.getLux() < pro_num2) {

                    if (DB > pro_num1) {
                        info.setBackgroundColor(Color.parseColor("#EF9A9A"));
                        //判闪
                        if (sw_ch1) {
                            light_flash();
                        } else {
                            light_on();
                            other();
                        }

                        handler.removeCallbacks(task);
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                //新线程操作
                                try {
                                    Thread.sleep(pro_num3 * 1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                handler.post(task);
                                light_off();
                                flash = false;

                            }
                        }.start();


                    }
                }
            }

        }
    };


    //闪光
    public void light_flash() {
        flash = true;
        new Thread() {
            @Override
            public void run() {
                super.run();
                //新线程操作

                while (flash) {

                    light_on();

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    light_off();

                    try {
                        Thread.sleep(pro_num4 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }


            }
        }.start();
    }


    private Handler handle = new Handler();
    private Runnable tas = new Runnable() {

        @TargetApi(Build.VERSION_CODES.M)
        public void run() {
            handler.postDelayed(this, 1000);//设置循环时间，此处是5秒
            if (!listen_all) {
                info.setBackgroundColor(Color.parseColor("#EF9A9A"));
            }
            info.setText("Sound " + DB + "/" + pro_num1 + "\nLight " + lsm.getLux() + "/" + pro_num2 + "\nTime " + pro_num3);
            RefreshVar();
        }
    };


    public void other() {
        if (sw_ch2) {
            //震动
            Vibrator vibrator = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);
            vibrator.vibrate(800);
        }

        if (sw_ch4) {
            //屏幕光
            changeAppBrightness(255);
        }
    }


    /**
     * 改变App当前Window亮度
     *
     * @param brightness
     */
    public void changeAppBrightness(int brightness) {
        Window window = this.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (brightness == -1) {
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            lp.screenBrightness = (brightness <= 0 ? 1 : brightness) / 255f;
        }
        window.setAttributes(lp);
    }


    /**
     * 获得系统亮度
     *
     * @return
     */
    private int getSystemBrightness() {
        int systemBrightness = 0;
        try {
            systemBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return systemBrightness;
    }


    private void init_val() {
        //控件实例化

        info = findViewById(R.id.info);
        lay1_1 = findViewById(R.id.lay1_1);
        lay1_2 = findViewById(R.id.lay1_2);

        lay2_1 = findViewById(R.id.lay2_1);
        lay2_2 = findViewById(R.id.lay2_2);
        lay2_3 = findViewById(R.id.lay2_3);

        lay3_1 = findViewById(R.id.lay3_1);
        lay3_2 = findViewById(R.id.lay3_2);

        lay4_1 = findViewById(R.id.lay4_1);
        lay4_2 = findViewById(R.id.lay4_2);
        lay4_3 = findViewById(R.id.lay4_3);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void init() {
        //初始化
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        lsm = LightSensorManager.getInstance();
        lsm.start(getApplicationContext());

        //开启声音监听 ...
        AudioRecordDemo au = new AudioRecordDemo();
        au.getNoiseLevel();

        handle.post(tas);


        SharedPreferences sp = this.getSharedPreferences("data", 0);


        pro_num1 = sp.getInt("pro_num1", 60);
        pro_num2 = sp.getInt("pro_num2", 100);
        pro_num3 = sp.getInt("pro_num3", 10);
        //pro_num4 = sp.getInt("pro_num4", 0);

        sw_ch1 = sp.getBoolean("sw_ch1", false);
        sw_ch2 = sp.getBoolean("sw_ch2", false);
        sw_ch3 = sp.getBoolean("sw_ch3", false);
        sw_ch4 = sp.getBoolean("sw_ch4", false);


        lay2_1.setProgress(sp.getInt("pro_num1", 0));
        lay2_2.setProgress(sp.getInt("pro_num2", 0));
        lay2_3.setProgress(sp.getInt("pro_num3", 0));
        //lay3_2.setProgress(sp.getInt("pro_num4", 0));

        lay3_1.setChecked(sp.getBoolean("sw_ch1", false));
        lay4_1.setChecked(sp.getBoolean("sw_ch2", false));
        //lay4_2.setChecked(sp.getBoolean("sw_ch3", false));
        lay4_3.setChecked(sp.getBoolean("sw_ch4", false));


        lay4_2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });


        lay2_1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pro_num1 = progress + 1;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        lay2_2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pro_num2 = progress + 1;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        lay2_3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pro_num3 = progress + 1;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        lay3_2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pro_num4 = progress + 1;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    public void listen_start(View view) {
        //总控
        listen_all = true;
        onoff();
        handler.post(task);//立即调用

    }

    @TargetApi(Build.VERSION_CODES.M)
    public void listen_stop(View view) {
        //总控
        light_off();
        listen_all = false;
        onoff();
        handler.removeCallbacks(task);
    }


    public void onoff() {
        RefreshVar();
        if (listen_all) {
            lay1_1.setEnabled(false);
            lay1_2.setEnabled(true);
        } else {
            lay1_1.setEnabled(true);
            lay1_2.setEnabled(false);
        }

        //showText("声控灯监听 - " + listen_all);
    }

    public void showText(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void RefreshVar() {
        //刷新变量
        sw_ch1 = lay3_1.isChecked();
        sw_ch2 = lay4_1.isChecked();
        sw_ch3 = lay4_2.isChecked();
        sw_ch4 = lay4_3.isChecked();

    }


    @Override
    protected void onPause() {
        SharedPreferences sp = this.getSharedPreferences("data", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("pro_num1", pro_num1);
        editor.putInt("pro_num2", pro_num2);
        editor.putInt("pro_num3", pro_num3);
        //editor.putInt("pro_num4", pro_num4);
        editor.putBoolean("sw_ch1", sw_ch1);
        editor.putBoolean("sw_ch2", sw_ch2);
        editor.putBoolean("sw_ch3", sw_ch3);
        editor.putBoolean("sw_ch4", sw_ch4);

        editor.commit();

        super.onPause();
    }

}
