package club.labcoders.playback;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import club.labcoders.playback.api.ApiManager;
import club.labcoders.playback.api.AuthApi;
import club.labcoders.playback.api.AuthManager;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class AuthActivity extends AppCompatActivity {


    @BindView(R.id.username)
    EditText username;

    @BindView(R.id.password)
    EditText password;

    @BindView(R.id.loginButton)
    Button loginButton;

    private AuthApi api;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            String token = cur.getString(cur.getColumnIndex("token"));
            api.ping(token)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(pong -> {
                        if (pong.isValid()) {
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    });
        }
    }

    @OnClick(R.id.loginButton)
    public void login() {
        api.auth(username.toString(), password.toString())
                .observeOn(Schedulers.io())
                .subscribe(authResult -> {
                    if (authResult.wasSuccess()) {
                        ContentValues vals = new ContentValues(2);
                        vals.put("id", 1);
                        vals.put("token", authResult.getToken());
                        db.insert("session", null, vals);

                        ApiManager.initialize(authResult.getToken());

                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to login.", Toast.LENGTH_SHORT);
                        username.setText("");
                        password.setText("");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}

