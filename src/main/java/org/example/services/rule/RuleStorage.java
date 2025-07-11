package org.example.services.rule;

import org.example.services.rule.com.ComRule;
import org.springframework.lang.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class RuleStorage {
    private static final ConcurrentMap<Integer, List<ComRule>> comRules = new ConcurrentHashMap<>();
    private static final RuleStorage instance;
    private static final ConcurrentHashMap<String, ComRule> ruleStorage = new ConcurrentHashMap<>();

    static {
        instance = new RuleStorage();
        instance.loadRulesFromDisk();
    }

    public static RuleStorage getInstance() {
        return instance;
    }

    public void addRule(ComRule rule){
        System.out.println("В коллекцию записано правило");
        ruleStorage.put(rule.getRuleId(), rule);
    }

    public void addAndPairRule(int clientId, ComRule rule) {
        if(ruleStorage.contains(rule)){
            System.out.println("Повторное добавление правила");
        }else{
            addRule(rule);
        }

        comRules.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(rule);
    }

    public void registryRule(int clientId, ComRule rule) {
        if(! ruleStorage.contains(rule)){
            System.out.println("Правило не было добавлено ранее");
            return;
        }
        comRules.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(rule);
        System.out.println("Клиент " + clientId + " связан с правилом " + rule.getRuleId() + " des " + rule.getDescription());
    }

    public List<ComRule> getAllRules() {
        List<ComRule> forReturn = new ArrayList<>();
        for (ComRule value : ruleStorage.values()) {
            forReturn.add(value);
        }
        return forReturn;
    }

    public  ConcurrentMap<Integer, List<ComRule>> getAllBounds(){
        return comRules;
    }

    /**
     * Возвращает список правил для клиента или null, если правила не найдены
     * @return список правил или null
     */
    @Nullable
    public List<ComRule> getRulesForClient(int clientId) {
        return comRules.getOrDefault(clientId, null); // Теперь явно возвращаем null
    }
    

    public  Set<Integer> findClientsByRule(String ruleId) {
        return comRules.entrySet().stream()
            .filter(entry -> entry.getValue().stream().anyMatch(r -> r.getRuleId().equals(ruleId)))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }
    
    public void saveRulesToDisk() {
        // Сериализация всех правил в файлы
        ruleStorage.values().forEach(rule -> {
            String fileName = "rules/" + rule.getRuleId() + ".rule";
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
                oos.writeObject(rule);
            } catch (IOException e) { /* обработка ошибки */ }
        });
    }
    
    public void loadRulesFromDisk() {
        // Загрузка всех правил из директории
        File rulesDir = new File("rules");
        if(rulesDir.exists() && rulesDir.isDirectory()) {
            File [] files = rulesDir.listFiles();
            if(files == null){
                return;
            }else{
                for (File file : rulesDir.listFiles((dir, name) -> name.endsWith(".rule"))) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                        ComRule rule = (ComRule) ois.readObject();
                        ruleStorage.put(rule.getRuleId(), rule);
                    } catch (Exception e) { /* обработка ошибки */ }
                }
            }
        }else {
            if (rulesDir.canWrite()) {
                rulesDir.delete();
            }
            rulesDir.mkdir();
        }
    }
}