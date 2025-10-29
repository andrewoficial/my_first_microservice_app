package org.example.services.rule;

import org.apache.log4j.Logger;
import org.example.gui.RuleManagmentDialog;
import org.example.services.rule.com.ComRule;
import org.springframework.lang.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class RuleStorage {
    private static final Logger log = Logger.getLogger(RuleStorage.class);
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

    public void registryRule(int clientId, ComRule rule) {
        if(rule == null){
            log.warn("Rule is null");
            return;
        }
        if(! ruleStorage.contains(rule)){
            System.out.println("Правило не было добавлено ранее");
            return;
        }
        comRules.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(rule);
        System.out.println("Клиент " + clientId + " связан с правилом " + rule.getRuleId() + " des " + rule.getDescription());
    }

    public ComRule findRule(int selectionNumber) {
        if(selectionNumber >= ruleStorage.size()){
            throw new IllegalArgumentException("Rule with number " + selectionNumber + " not found");
        }else{
            int cnt = 0;
            for (Map.Entry<String, ComRule> stringComRuleEntry : ruleStorage.entrySet()) {
                if(cnt == selectionNumber){
                    return stringComRuleEntry.getValue();
                }
                cnt++;
            }
            throw new IllegalArgumentException("Rule with number " + selectionNumber + " not found");
        }
    }
    public void unRegistryRule(int clientId, ComRule rule) {
        if(rule == null){
            log.warn("Rule is null");
            return;
        }
        if(! ruleStorage.contains(rule)){
            log.warn("Rule is already add");
            return;
        }
        if(comRules.containsKey(clientId)){
            List<ComRule> foundetRulesOld = comRules.get(clientId);
            List<ComRule> foundetRulesNew = new ArrayList<>(foundetRulesOld.size() - 1);
            boolean wasRemoved = false;
            for (ComRule foundetRule : foundetRulesOld) {
                log.info("Сравниваю правило " + foundetRule.getRuleId() + " с " + rule.getRuleId());
                if(foundetRule.getRuleId().equalsIgnoreCase(rule.getRuleId())){
                    foundetRulesOld.remove(foundetRule);
                    log.info("Правило удалено из коллекции " + clientId);
                    wasRemoved = true;
                }else{
                    foundetRulesNew.add(foundetRule);
                }
            }
            if(wasRemoved){
                log.info("Коллекция присвоена клиенту " + clientId);
//                if(comRules.containsKey(clientId)){
//                    comRules.remove(clientId);
//                    comRules.put(clientId, foundetRulesNew);
//                }
            }else{
                log.warn("Правило не удалено для клиента " + clientId + " id правила " + rule.getRuleId());
            }

        }else{
            log.warn("Попытка удаления правила для клиента, отсутствующего в хранилище");
        }

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
        log.info("Saving rules to disk");

        File rulesDir = new File("rules");

        try {
            // Clear folder more safely
            if (rulesDir.exists()) {
                if (!rulesDir.isDirectory()) {
                    log.error("'rules' exists but is not a directory");
                    return;
                }

                // Delete all files in the directory instead of deleting the directory itself
                File[] files = rulesDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            log.warn("Failed to delete file: {"+file.getName()+"}");
                        }
                    }
                }
            } else {
                // Create directory if it doesn't exist
                if (!rulesDir.mkdirs()) {
                    log.error("Failed to create rules directory");
                    return;
                }
            }

            log.info("Folder cleared");

            // Serialize all rules to files
            int successCount = 0;
            int totalCount = ruleStorage.size();

            for (ComRule rule : ruleStorage.values()) {
                String fileName = "rules/" + rule.getRuleId() + ".rule";
                File ruleFile = new File(fileName);

                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ruleFile))) {
                    oos.writeObject(rule);
                    successCount++;
                    log.debug("Rule saved successfully: {"+fileName+"}");
                } catch (IOException e) {
                    log.error("Failed to save rule {"+rule.getRuleId() +"}: {"+e.getMessage()+"}");
                    // Consider if you want to continue saving other rules or abort
                }
            }

            log.info("Successfully saved {"+successCount+"}/{"+totalCount+"} rules to disk");

        } catch (SecurityException e) {
            log.error("Security exception while accessing rules directory: {"+e.getMessage()+"}");
        }
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