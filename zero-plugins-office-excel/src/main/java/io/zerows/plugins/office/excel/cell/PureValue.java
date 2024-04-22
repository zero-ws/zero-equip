package io.zerows.plugins.office.excel.cell;

public class PureValue implements ExValue {

    @Override
    @SuppressWarnings("all")
    public Object to(Object value) {
        return value;
    }
}
