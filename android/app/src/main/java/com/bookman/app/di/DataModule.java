package com.bookman.app.di;

import android.content.Context;
import com.bookman.app.data.api.AuthApi;
import com.bookman.app.data.auth.AuthRepository;
import com.bookman.app.data.auth.AuthRepositoryImpl;
import com.bookman.app.data.session.SessionStore;
import com.bookman.app.data.session.SharedPrefsSessionStore;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public final class DataModule {

    private DataModule() {
    }

    @Provides
    @Singleton
    static Executor ioExecutor() {
        return Executors.newFixedThreadPool(4);
    }

    @Provides
    @Singleton
    static SessionStore sessionStore(@ApplicationContext Context context) {
        return new SharedPrefsSessionStore(context);
    }

    @Provides
    @Singleton
    static AuthRepository authRepository(AuthApi api, SessionStore sessionStore, Executor io) {
        return new AuthRepositoryImpl(api, sessionStore, io);
    }
}
