package com.example.smartfarm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/*import com.bumptech.glide.Glide;*/

public class LoginResultActivity extends AppCompatActivity {

    private TextView tv_nickname;
    private ImageView iv_profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_result);

        Intent intent = getIntent();

        tv_nickname = findViewById(R.id.tv_nickname);
        tv_nickname.setText(intent.getStringExtra("nicmName"));

        iv_profile = findViewById(R.id.iv_profile);
        /*Glide.with(this).load(intent.getStringExtra("photoUrl")).into(iv_profile); // 프로필 url을 이미지에 셋팅*/

    }
}
