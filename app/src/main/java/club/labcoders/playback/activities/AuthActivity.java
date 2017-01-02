package club.labcoders.playback.activities;

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
import club.labcoders.playback.misc.TrivialErrorHandler;
import club.labcoders.playback.services.DatabaseService;
import club.labcoders.playback.R;
import club.labcoders.playback.api.ApiManager;
import club.labcoders.playback.api.AuthApi;
import club.labcoders.playback.api.AuthManager;
import club.labcoders.playback.api.models.ApiAuthPing;
import club.labcoders.playback.api.models.ApiAuthResult;
import club.labcoders.playback.api.models.ApiAuthenticationRequest;
import club.labcoders.playback.db.models.DbSessionToken;
import club.labcoders.playback.misc.Box;
import club.labcoders.playback.misc.rx.RxServiceBinding;
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

        tryExistingToken();
    }

    private void tryExistingToken() {
        // Try to get session token from local DB
        final Box<String> tokenBox = new Box<>();
        final Subscription sub = observeDatabaseService()
                .doOnNext($ -> Timber.d("Getting token."))
                .flatMap(DatabaseService::getToken)
                .map(DbSessionToken::getToken)
                .flatMap(s -> {
                    Timber.d("Got token %s", s);
                    if(s == null)
                        return Observable.just(null);
                    tokenBox.setValue(s);
                    return AuthManager.getInstance()
                            .getApi()
                            .ping(new ApiAuthPing(s))
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
                        },
                        new TrivialErrorHandler("auth pong"),
                        () -> Timber.d("Donerino.")
                );
        subscriptions.add(sub);
    }

    private Observable<DatabaseService> observeDatabaseService() {
        return new RxServiceBinding<DatabaseService.DatabaseServiceBinder>(
                this,
                new Intent(this, DatabaseService.class),
                Service.BIND_AUTO_CREATE)
                .binder(true)
                .map(DatabaseService.DatabaseServiceBinder::getService);
    }

    @OnClick(R.id.loginButton)
    public synchronized void login() {
        if(authSubscription != null && !authSubscription.isUnsubscribed()) {
            Timber.d("Ignoring multiple concurrent login attempts.");
            return;
        }
        final String username = this.username.getText().toString();
        final String password = this.password.getText().toString();
        final Box<ApiAuthResult> authResultBox = new Box<>();
        final Subscription sub = api.auth(
                new ApiAuthenticationRequest(username, password))
                .flatMap(authResult -> {
                    authResultBox.setValue(authResult);
                    return new RxServiceBinding<DatabaseService.DatabaseServiceBinder>(
                            AuthActivity.this,
                            new Intent(AuthActivity.this, DatabaseService.class),
                            Service.BIND_AUTO_CREATE
                    ).binder(true);
                })
                .map(DatabaseService.DatabaseServiceBinder::getService)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .filter($ -> {
                    boolean result = authResultBox.getValue().getSuccess();
                    if(!result) {
                        Toast.makeText(
                                this,
                                "Failed to login.",
                                Toast.LENGTH_SHORT)
                                .show();
                        this.password.setText("");
                    }
                    return result;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext($ -> Toast.makeText(
                        this,
                        "Logged in!",
                        Toast.LENGTH_LONG)
                        .show()
                )
                .observeOn(Schedulers.io())
                .flatMap(databaseService -> {
                    Timber.d("About to upsert token.");
                    return databaseService.upsertToken(
                            authResultBox.getValue()
                                    .getToken()
                    );
                })
                .map($ -> {
                    ApiManager.initialize(authResultBox.getValue().getToken());
                    return null;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        $ -> {
                            transitionToMainActivity();
                        },
                        new TrivialErrorHandler("auth")
                );
        subscriptions.add(sub);
    }

    private void transitionToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        Timber.d("Destroying AuthActivity");
        subscriptions.unsubscribe();
        super.onDestroy();
    }
}

