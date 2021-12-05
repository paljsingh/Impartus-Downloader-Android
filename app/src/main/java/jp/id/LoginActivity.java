package jp.id;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.EditText;

import jp.id.activities.VideoActivity;
import jp.id.core.Impartus;
import jp.id.core.Utils;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        setContentView(R.layout.activity_login);

        // if we have a valid session saved, jump to video activity.
        String sessionToken = Utils.getSessionTokenFromPrefs(this);
        String baseUrl = Utils.getUrlFromPrefs(this);
        if (sessionToken != null && Impartus.isValidSession(sessionToken) && baseUrl != null) {
            launchVideoActivity();
        }
    }

    public void launchVideoActivity() {
        Intent intent = new Intent(LoginActivity.this, VideoActivity.class);
        finish();
        startActivity(intent);
    }

    public void onClickLoginButton(View view) {
        String baseUrl = ((EditText) findViewById(R.id.url)).getText().toString();
        String username = ((EditText) findViewById(R.id.username)).getText().toString();
        String password = ((EditText) findViewById(R.id.password)).getText().toString();

        Impartus impartus = new Impartus(baseUrl, this.getCacheDir());
        boolean success = impartus.login(username, password);
        if (success) {
            // save session token, and launch video activity.
            String sessionToken = impartus.getSessionToken();
            Utils.saveSharedPrefs(this, baseUrl, sessionToken);

            launchVideoActivity();
        }
    }
}