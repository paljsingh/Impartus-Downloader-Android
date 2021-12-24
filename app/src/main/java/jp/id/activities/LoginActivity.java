package jp.id.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import jp.id.ContextManager;
import jp.id.R;
import jp.id.core.Impartus;
import jp.id.core.Utils;
import jp.id.model.AppLogs;
import jp.id.BuildConfig;

public class LoginActivity extends AppCompatActivity {

    private final String tag = "LoginActivity";
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
        ContextManager.setContext(this.getBaseContext());
        setContentView(R.layout.activity_login);

        refreshDataIfRequired();

        // if we have a valid session saved, jump to video activity.
        String sessionToken = Utils.getSessionTokenFromPrefs(this);
        String baseUrl = Utils.getUrlFromPrefs(this);
        if (sessionToken != null && Impartus.isValidSession(sessionToken) && baseUrl != null) {
            AppLogs.info(tag, "Logging in to impartus site using existing session-token.");
            launchVideoActivity();
        }
    }

    private void refreshDataIfRequired() {
        final int currentVersion = BuildConfig.VERSION_CODE;
        final int savedVersion = Integer.parseInt(Utils.getDataKey(this, "version", "0"));
        if (savedVersion < currentVersion) {
            Utils.deleteDataKeys();
        }

        Utils.setDefaultDataKeys();

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

        TextView failedLogin = findViewById(R.id.failed_login);
        failedLogin.setVisibility(TextView.INVISIBLE);

        Impartus impartus = new Impartus(baseUrl, this.getCacheDir());

        AppLogs.info(tag, String.format("Logging in to impartus site with username: %s", username));
        boolean success = impartus.login(username, password);
        if (success) {
            // save session token, and launch video activity.
            String sessionToken = impartus.getSessionToken();
            Utils.saveSession(this, baseUrl, sessionToken);

            launchVideoActivity();
        } else {
            final String error = "Error logging to impartus, please check your login credentials.";
            AppLogs.error(tag, error);
            Toast.makeText(view.getContext(), error, Toast.LENGTH_LONG).show();
            failedLogin.setVisibility(TextView.VISIBLE);
        }
    }
}