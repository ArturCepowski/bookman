package com.bookman.app.di;

import com.bookman.app.BuildConfig;
import com.bookman.app.data.api.ApartmentApi;
import com.bookman.app.data.api.AuthApi;
import com.bookman.app.data.api.AuthInterceptor;
import com.bookman.app.data.api.MemberApi;
import com.bookman.app.data.api.ReservationApi;
import com.bookman.app.data.api.UserApi;
import com.bookman.app.data.session.SessionStore;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public final class NetworkModule {

    private NetworkModule() {
    }

    @Provides
    @Singleton
    static OkHttpClient okHttpClient(SessionStore sessionStore) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(sessionStore));
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            builder.addInterceptor(logging);
        }
        return builder.build();
    }

    @Provides
    @Singleton
    static Retrofit retrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    static AuthApi authApi(Retrofit retrofit) {
        return retrofit.create(AuthApi.class);
    }

    @Provides
    @Singleton
    static UserApi userApi(Retrofit retrofit) {
        return retrofit.create(UserApi.class);
    }

    @Provides
    @Singleton
    static ApartmentApi apartmentApi(Retrofit retrofit) {
        return retrofit.create(ApartmentApi.class);
    }

    @Provides
    @Singleton
    static ReservationApi reservationApi(Retrofit retrofit) {
        return retrofit.create(ReservationApi.class);
    }

    @Provides
    @Singleton
    static MemberApi memberApi(Retrofit retrofit) {
        return retrofit.create(MemberApi.class);
    }
}
