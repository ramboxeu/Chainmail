package io.github.ramboxeu.chainmail.container;

public interface IModInstanceWrapper {
    void runInitialization();
    void runClientInitialization();
    void runServerInitialization();
}
