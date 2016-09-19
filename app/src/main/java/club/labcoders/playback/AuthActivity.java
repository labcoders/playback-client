package club.labcoders.playback;

import android.app.Service;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
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
import club.labcoders.playback.api.models.AuthPing;
import club.labcoders.playback.api.models.AuthenticationRequest;
import club.labcoders.playback.db.DatabaseService;
import rx.Observable;
import rx.Subscription;
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
    private Subscription authSubscription = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        setContentView(R.layout.activity_auth);

        ButterKnife.setDebug(true);
        ButterKnife.bind(this);


        // Try to get session token from local DB
        final Box<String> tokenBox = new Box<>();
        new RxServiceBinding<DatabaseService.DatabaseServiceBinder>(
                this,
                new Intent(this, DatabaseService.class),
                Service.BIND_AUTO_CREATE).binder()
                .map(DatabaseService.DatabaseServiceBinder::getService)
                .flatMap(DatabaseService::getToken)
                .flatMap(s -> {
                    if(s == null)
                        return Observable.just(null);
                    tokenBox.setValue(s);
                    return AuthManager.getInstance()
                            .getApi()
                            .ping(new AuthPing(s));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(authPong -> {
                    if(authPong == null) {
                        Timber.d("No token in DB.");
                    }
                    else if(authPong.isValid()) {
                        ApiManager.initialize(tokenBox.getValue());
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                    else {
                        Timber.d("Token is too old.");
                    }
                });
    }

    @OnClick(R.id.loginButton)
    public synchronized void login() {
        if(authSubscription != null && !authSubscription.isUnsubscribed()) {
            Timber.d("Ignoring multiple concurrent login attempts.");
            return;
        }
        final String username = this.username.getText().toString();
        final String password = this.password.getText().toString();
        authSubscription = api.auth(
                new AuthenticationRequest(username, password))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(authResult -> {
                    if (authResult.getSuccess()) {
                        ContentValues vals = new ContentValues(1);
                        vals.put("token", authResult.getToken());
                        db.updateWithOnConflict(
                                "session",
                                vals,
                                "id = 1",
                                new String[0],
                                db.CONFLICT_REPLACE
                        );

                        ApiManager.initialize(authResult.getToken());

                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                    else {
                        Toast.makeText(this, "Failed to login.", Toast
                                .LENGTH_SHORT);
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

