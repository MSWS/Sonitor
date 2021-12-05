package xyz.msws.admintools.parsers;

import xyz.msws.admintools.Monitor;

public abstract class Parser {
    protected Monitor monitor;

    public Parser(Monitor monitor) {
        this.monitor = monitor;
    }

    abstract public void parse(String line);
}
