package com.dday.wedding.common;

public class BadRequest extends RuntimeException {
    public BadRequest(String code) { super(code); }
}
