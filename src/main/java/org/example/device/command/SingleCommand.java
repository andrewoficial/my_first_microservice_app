package org.example.device.command;

import org.example.services.AnswerValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SingleCommand implements CommandBuilder, CommandParser {
    private String guiName;                // For UI display (e.g., "setConc")
    private String description;            // "setConc [value] - Установить калибровку span"
    private String mapKey;                 // Key for search in map (normalized name)
    private byte[] baseBody;               // Base command bytes (e.g., {0x10, 0x13, 0x01, 0x10, 0x1F} for binary; or ASCII bytes)
    private CommandBuilder builder;        // Function to generate full byte[] (implements interface)
    private CommandParser parser;          // Function to parse response
    private int expectedBytes;             // Expected response length
    private CommandType type;              // ASCII/BINARY/JSON
    private List<ArgumentDescriptor> arguments = new ArrayList<>();  // For dynamic args

    // Constructor for new full version
    public SingleCommand(String guiName, String description, String mapKey, byte[] baseBody,
                         CommandBuilder builder, CommandParser parser, int expectedBytes, CommandType type) {
        this.guiName = guiName;
        this.description = description;
        this.mapKey = mapKey != null ? mapKey : guiName;  // Default to guiName if null
        this.baseBody = baseBody;
        this.builder = builder;
        this.parser = parser;
        this.expectedBytes = expectedBytes;
        this.type = type;
    }

    // Backward compat constructor (for your 16 old protocols)
    public SingleCommand(String name, String description, Function<byte[], AnswerValues> parseFunction, int expectedBytes) {
        this(name, description, name, name.getBytes(),  // Assume ASCII baseBody
             args -> { if (!args.isEmpty()) throw new IllegalArgumentException("No args expected"); return name.getBytes(); },  // Simple builder
             parseFunction::apply, expectedBytes, CommandType.ASCII);
    }

    // Methods...
    public String getGuiName() { return guiName; }
    public String getDescription() { return description; }
    public String getMapKey() { return mapKey; }
    public byte[] getBaseBody() { return baseBody; }
    public int getExpectedBytes() { return expectedBytes; }
    public CommandType getType() { return type; }
    public List<ArgumentDescriptor> getArguments() { return arguments; }
    public void addArgument(ArgumentDescriptor arg) { arguments.add(arg); }

    // Implement CommandBuilder
    @Override
    public byte[] build(Map<String, Object> args) {
        // Validate args
        for (ArgumentDescriptor desc : arguments) {
            Object val = args.get(desc.getName());
            if (val == null) val = desc.getDefaultValue();
            if (!desc.validate(val)) throw new IllegalArgumentException("Invalid arg: " + desc.getName());
        }
        return builder.build(args);  // Delegate to custom builder
    }

    // Implement CommandParser
    @Override
    public AnswerValues parse(byte[] response) {
        // Optional pre-process based on type
        switch (type) {
            case ASCII: // e.g., convert to String if needed
                return parser.parse(response);
            case JSON: // e.g., Gson parse first
                // Assume parser handles
            default:
                return parser.parse(response);
        }
    }

    // For old getResult (compat)
    public AnswerValues getResult(byte[] arr) {
        return parse(arr);
    }
}