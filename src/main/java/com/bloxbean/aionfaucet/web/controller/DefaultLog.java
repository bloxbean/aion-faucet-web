package com.bloxbean.aionfaucet.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.aion4j.avm.helper.api.Log;

@Slf4j
public class DefaultLog implements Log {
    @Override
    public void info(String s) {
        log.info(s);
    }

    @Override
    public void debug(String s) {
        log.debug(s);
    }

    @Override
    public void error(String s) {
        log.error(s);
    }

    @Override
    public void warn(String s) {
        log.warn(s);
    }

    @Override
    public void info(String s, Throwable throwable) {
        log.info(s, throwable);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        log.debug(s, throwable);
    }

    @Override
    public void error(String s, Throwable throwable) {
        log.error(s, throwable);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        log.warn(s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }
}
