package com.bookman.app.di;

import android.content.Context;
import com.bookman.app.data.api.ApartmentApi;
import com.bookman.app.data.api.AuthApi;
import com.bookman.app.data.api.MemberApi;
import com.bookman.app.data.api.ReservationApi;
import com.bookman.app.data.api.UserApi;
import com.bookman.app.data.apartment.ApartmentRepository;
import com.bookman.app.data.apartment.ApartmentRepositoryImpl;
import com.bookman.app.data.auth.AuthRepository;
import com.bookman.app.data.auth.AuthRepositoryImpl;
import com.bookman.app.data.member.MemberRepository;
import com.bookman.app.data.member.MemberRepositoryImpl;
import com.bookman.app.data.profile.ProfileRepository;
import com.bookman.app.data.reservation.ReservationRepository;
import com.bookman.app.data.reservation.ReservationRepositoryImpl;
import com.bookman.app.data.profile.ProfileRepositoryImpl;
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

    @Provides
    @Singleton
    static ProfileRepository profileRepository(UserApi api, Executor io) {
        return new ProfileRepositoryImpl(api, io);
    }

    @Provides
    @Singleton
    static ApartmentRepository apartmentRepository(ApartmentApi api, Executor io) {
        return new ApartmentRepositoryImpl(api, io);
    }

    @Provides
    @Singleton
    static ReservationRepository reservationRepository(ReservationApi api, Executor io) {
        return new ReservationRepositoryImpl(api, io);
    }

    @Provides
    @Singleton
    static MemberRepository memberRepository(MemberApi api, Executor io) {
        return new MemberRepositoryImpl(api, io);
    }
}
