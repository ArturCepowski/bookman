package com.bookman.app.data.apartment;

import androidx.core.util.Consumer;
import com.bookman.app.common.Result;
import java.util.List;

public interface ApartmentRepository {

    void list(Consumer<Result<List<ApartmentSummary>>> callback);

    void get(long id, Consumer<Result<ApartmentDetails>> callback);

    void create(ApartmentInput input, Consumer<Result<ApartmentDetails>> callback);

    void update(long id, ApartmentInput input, Consumer<Result<ApartmentDetails>> callback);

    void delete(long id, Consumer<Result<Boolean>> callback);
}
