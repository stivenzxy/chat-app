package com.serverApplication.commands.contract;

public interface Command <T, R>{
    R execute(T request);
}
