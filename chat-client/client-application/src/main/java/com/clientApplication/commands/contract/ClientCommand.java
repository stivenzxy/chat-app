package com.clientApplication.commands.contract;

public interface ClientCommand<T, R>{
    R execute(T request);
}
