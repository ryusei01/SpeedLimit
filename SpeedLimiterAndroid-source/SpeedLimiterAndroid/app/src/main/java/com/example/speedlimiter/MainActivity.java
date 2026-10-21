package com.example.speedlimiter;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    private LocalProxy proxy;
    private TextView status;
    private EditText speed;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 48, 32, 24);
        TextView title = new TextView(this); title.setText("Speed Limiter - Wi-Fi Proxy"); title.setTextSize(23); title.setTypeface(null, Typeface.BOLD); root.addView(title);
        TextView info = new TextView(this);
        info.setText("Wi-Fi の手動プロキシを 127.0.0.1 / 8888 に設定してください。\nモバイルデータ通信・VPN・アプリ別制限には未対応です。\nこのアプリを開いた状態で利用してください。");
        info.setPadding(0,24,0,24); root.addView(info);
        speed = new EditText(this); speed.setInputType(2); speed.setText("1000"); speed.setHint("速度 (kbps)"); root.addView(speed);
        Button start = new Button(this); start.setText("開始"); root.addView(start);
        Button stop = new Button(this); stop.setText("停止"); root.addView(stop);
        status = new TextView(this); status.setPadding(0,20,0,0); status.setText("停止中"); root.addView(status);
        setContentView(root);
        start.setOnClickListener(v -> {
            try {
                int kbps = Integer.parseInt(speed.getText().toString());
                if (kbps < 64 || kbps > 100000) throw new IllegalArgumentException("64〜100000 kbpsで指定してください");
                if (proxy != null) proxy.close();
                proxy = new LocalProxy(8888, kbps); proxy.start();
                status.setText("起動中: " + kbps + " kbps (Wi-Fi プロキシのみ)");
            } catch (Exception e) { status.setText("エラー: " + e.getMessage()); }
        });
        stop.setOnClickListener(v -> { if (proxy != null) proxy.close(); proxy = null; status.setText("停止中"); });
    }
    @Override protected void onDestroy() { if (proxy != null) proxy.close(); super.onDestroy(); }
}
