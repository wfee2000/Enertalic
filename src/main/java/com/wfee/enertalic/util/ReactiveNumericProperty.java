package com.wfee.enertalic.util;

public class ReactiveNumericProperty extends ReactiveProperty<Long> {
    public ReactiveNumericProperty(long value) {
        super(value);
    }

    public void add(long value) {
        modify(number -> number + value, UpdateType.Increment);
    }

    public void subtract(long value) {
        modify(number -> number - value, UpdateType.Decrement);
    }
}
