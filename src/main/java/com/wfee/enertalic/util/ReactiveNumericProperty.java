package com.wfee.enertalic.util;

public class ReactiveNumericProperty extends ReactiveProperty<Long> {
    public ReactiveNumericProperty(long value) {
        super(value);
    }

    public void add(long value) {
        set(get() + value, UpdateType.Increment);
    }

    public void subtract(long value) {
        set(get() - value, UpdateType.Decrement);
    }
}
