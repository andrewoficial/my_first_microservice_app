package org.example.utilites;

public class ParseException extends Exception{
    private String string;
    public String getString(){return "["+string+"]";}

    public ParseException(String message, String string){

        super(message);
        this.string=string;
    }
}
