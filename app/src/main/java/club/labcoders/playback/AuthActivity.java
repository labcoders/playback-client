package club.labcoders.playback;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import club.labcoders.playback.api.ApiManager;
import club.labcoders.playback.api.AuthApi;
import club.labcoders.playback.api.AuthManager;
import club.labcoders.playback.api.models.AuthPing;
import club.labcoders.playback.api.models.AuthenticationRequest;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class AuthActivity extends AppCompatActivity {
    @BindView(R.id.usernameField)
    EditText username;

    @BindView(R.id.passwordField)
    EditText password;

    @BindView(R.id.loginButton)
    Button loginButton;

    private AuthApi api;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        setContentView(R.layout.activity_auth);

        ButterKnife.setDebug(true);
        ButterKnife.bind(this);

        db = openOrCreateDatabase("session", Context.MODE_PRIVATE, null);
        db.execSQL("create table if not exists session (id INT, token VARCHAR);");

        // Try to get session token from local DB
        Cursor cur = db.rawQuery("select * from session where id = 1;", null);

        api = AuthManager.getInstance().getApi();

        // Try to ping and see if token is still valid
        // If valid, start main activity
        // If not, stay on login page and continue
        // rendering UI.
        if (cur.getCount() > 0) {
            cur.moveToNext();
            final String token = cur.getString(cur.getColumnIndex("token"));
            api.ping(new AuthPing(token))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(pong -> {
                        if (pong.isValid()) {
                            ApiManager.initialize(token);
                            Timber.d("Initialized ApiManager");
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    });
        }
    }

    @OnClick(R.id.loginButton)
    public void login() {
        final String username = this.username.getText().toString();
        final String password = this.password.getText().toString();
        api.auth(new AuthenticationRequest(username, password))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(authResult -> {
                    if (authResult.getSuccess()) {
                        ContentValues vals = new ContentValues(2);
                        vals.put("id", 1);
                        vals.put("token", authResult.getToken());
                        db.insert("session", null, vals);

                        ApiManager.initialize(authResult.getToken());

                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to login.", Toast.LENGTH_SHORT);
                        this.password.setText("");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}

