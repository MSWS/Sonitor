package xyz.msws.admintools.parsers;

import xyz.msws.admintools.Monitor;
import xyz.msws.admintools.data.Config;

/**
 * Represents a parser
 */
public abstract class Parser {
    protected Monitor monitor;
    protected Config config;

    public Parser(Monitor monitor) {
        this.monitor = monitor;
        this.config = monitor.getConfig();
    }

    abstract public void parse(String line);
}
