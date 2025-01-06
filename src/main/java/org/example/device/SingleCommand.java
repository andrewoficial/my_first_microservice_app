package org.example.device;

import org.example.services.AnswerValues;

import java.util.function.Function;

public class SingleCommand {
    private  String name;
    private  String description;
    private  Function<byte [], AnswerValues> parseFunction;
    private  int expectedBytes = 0;

    public SingleCommand(String name, String description, Function<byte [], AnswerValues> parseFunction, int expectedBytes) {
                    this.description = description;
                    this.parseFunction = parseFunction;
                    this.name = name;
                    this.expectedBytes = expectedBytes;
    }

    public SingleCommand(String name, String description, Function<byte [], AnswerValues> parseFunction) {
        this.description = description;
        this.parseFunction = parseFunction;
        this.name = name;
        this.expectedBytes = 5000;
    }
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Function<byte[], AnswerValues> getParseFunction() {
        return parseFunction;
    }

    public int getExpectedBytes() {
        return expectedBytes;
    }

    public AnswerValues getResult(byte[] arr){
        return parseFunction.apply(arr);
    }

}
