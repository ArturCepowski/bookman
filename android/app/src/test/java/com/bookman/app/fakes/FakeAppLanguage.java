package com.bookman.app.fakes;

import com.bookman.app.common.AppLanguage;
import java.util.ArrayList;
import java.util.List;

public class FakeAppLanguage implements AppLanguage {

    public final List<String> applied = new ArrayList<>();

    @Override
    public void apply(String code) {
        applied.add(code);
    }
}
