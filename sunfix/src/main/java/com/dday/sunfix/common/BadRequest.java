package com.dday.sunfix.common;

/** 400 {code}. 화면은 code 로 라벨 err.{code} 를 찾아 보여준다. */
public class BadRequest extends RuntimeException {
    public BadRequest(String code) { super(code); }
}
