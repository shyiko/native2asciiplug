package com.github.shyiko.native2asciiplug;

import com.intellij.openapi.compiler.ValidityState;

import java.io.DataOutput;
import java.io.IOException;

public class EmptyValidityState implements ValidityState {

    public boolean equalsTo(ValidityState otherState) {
        return otherState == this;
    }

    public void save(DataOutput dataOutput) throws IOException {}
}
