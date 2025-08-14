package org.example.gui.accu10fd.table;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class AcuTableFileHandler {
    private final List<GasData> gasList = new ArrayList<>();

    public AcuTableFileHandler() throws IOException {
        loadTable();
    }

    private void loadTable() throws IOException {
        Path file = AcuTableCreator.getFilePath();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // пропускаем заголовок
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length != 5) continue; // некорректная строка
                try {
                    GasData gas = new GasData(
                        parts[0].trim(),
                        parts[1].trim(),
                        Double.parseDouble(parts[2].trim()),
                        Double.parseDouble(parts[3].trim()),
                        Double.parseDouble(parts[4].trim())
                    );
                    gasList.add(gas);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public List<GasData> getAll() {
        return Collections.unmodifiableList(gasList);
    }

    public List<String> getAllNames() {
        return gasList.stream().map(GasData::getName).collect(Collectors.toList());
    }

    public GasData getByName(String name) {
        return gasList.stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public GasData getByCode(String code) {
        return gasList.stream()
                .filter(g -> g.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}