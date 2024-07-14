package org.example.utilites;

import org.example.services.AnswerValues;

import java.util.List;
import java.util.function.Function;

public class SingleCommand {
    private  String name;
    private  String description;
    private  Function<byte [], AnswerValues> parseFunction;

    public SingleCommand(String name,
                  String description,
                  Function<byte [], AnswerValues> parseFunction){
                    this.description = description;
                    this.parseFunction = parseFunction;
                    this.name = name;}

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Function<byte[], AnswerValues> getParseFunction() {
        return parseFunction;
    }

    public AnswerValues getResult(byte[] arr){
        return parseFunction.apply(arr);
    }

}
