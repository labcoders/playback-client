package club.labcoders.playback;

import android.app.Activity;
import android.app.Service;
import android.os.Bundle;
import android.content.Intent;
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
import club.labcoders.playback.api.models.AuthResult;
import club.labcoders.playback.api.models.AuthenticationRequest;
import club.labcoders.playback.db.DatabaseService;
import club.labcoders.playback.misc.Box;
import club.labcoders.playback.misc.RxServiceBinding;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class AuthActivity extends Activity {
    @BindView(R.id.usernameField)
    EditText username;

    @BindView(R.id.passwordField)
    EditText password;

    @BindView(R.id.loginButton)
    Button loginButton;

    private AuthApi api;
    private Subscription authSubscription = null;
    private final CompositeSubscription subscriptions;

    public AuthActivity() {
        subscriptions = new CompositeSubscription();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.plant(new Timber.DebugTree());

        Timber.d("created auth activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        setContentView(R.layout.activity_auth);

        ButterKnife.setDebug(true);
        ButterKnife.bind(this);

        api = AuthManager.getInstance().getApi();

        // Try to get session token from local DB
        final Box<String> tokenBox = new Box<>();
        final Subscription sub = new RxServiceBinding<DatabaseService.DatabaseServiceBinder>(
                this,
                new Intent(this, DatabaseService.class),
                Service.BIND_AUTO_CREATE
        )
                .binder(true)
                .map(DatabaseService.DatabaseServiceBinder::getService)
                .flatMap(DatabaseService::getToken)
                .flatMap(s -> {
                    Timber.d("Got token %s", s);
                    if(s == null)
                        return Observable.just(null);
                    tokenBox.setValue(s);
                    return AuthManager.getInstance()
                            .getApi()
                            .ping(new AuthPing(s))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        authPong -> {
                            if (authPong == null) {
                                Timber.d("No token in DB.");
                            } else if (authPong.isValid()) {
                                ApiManager.initialize(tokenBox.getValue());
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                Timber.d("Token is too old.");
                            }
                        }
                );
        subscriptions.add(sub);
    }

    @OnClick(R.id.loginButton)
    public synchronized void login() {
        if(authSubscription != null && !authSubscription.isUnsubscribed()) {
            Timber.d("Ignoring multiple concurrent login attempts.");
            return;
        }
        final String username = this.username.getText().toString();
        final String password = this.password.getText().toString();
        final Box<AuthResult> authResultBox = new Box<>();
        final Subscription sub = api.auth(
                new AuthenticationRequest(username, password))
                .flatMap(authResult1 -> {
                    authResultBox.setValue(authResult1);
                    return new RxServiceBinding<DatabaseService.DatabaseServiceBinder>(
                            AuthActivity.this,
                            new Intent(AuthActivity.this, DatabaseService.class),
                            Service.BIND_AUTO_CREATE
                    ).binder(true);
                })
                .doOnNext(databaseServiceBinder -> Timber.d("Got DB binder."))
                .map(DatabaseService.DatabaseServiceBinder::getService)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(db -> {
                    final AuthResult authResult = authResultBox.getValue();
                    if (authResult.getSuccess()) {
                        Toast.makeText(this, "Logged in!", Toast.LENGTH_LONG).show();
                        Timber.d("About to upsert token.");
                        db.upsertToken(authResultBox.getValue().getToken());
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
        subscriptions.add(sub);
    }

    @Override
    protected void onDestroy() {
        Timber.d("Destroying AuthActivity");
        subscriptions.unsubscribe();
        super.onDestroy();
    }
}

