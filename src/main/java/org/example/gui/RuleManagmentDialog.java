package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.services.AnswerStorage;
import org.example.services.connectionPool.AnyPoolService;
import org.example.services.rule.ParamMetadata;
import org.example.services.rule.RuleFactory;
import org.example.services.rule.RuleStorage;
import org.example.services.rule.com.*;
import org.example.utilites.properties.MyProperties;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.example.services.rule.com.RuleType;

public class RuleManagmentDialog extends JDialog {
    private static final Logger log = Logger.getLogger(RuleManagmentDialog.class);
    private final RuleStorage ruleStorage = RuleStorage.getInstance();
    private final StringBuilder sb = new StringBuilder();
    private JPanel JP_RuleCreate;
    private JComboBox JC_SampleRules;
    private JButton BT_SelectTemplate;
    private JPanel JP_Arguments;
    private JButton BT_CreateRule;
    private JPanel JP_RuleAssociate;
    private JButton BT_Refresh;
    private JComboBox JC_Clients;
    private JComboBox JC_Rules;
    private JButton BT_AddPair;
    private JPanel JP_ExistingRules;
    private JComboBox JC_ExistPair;
    private JButton BT_RemovePair;
    private JPanel mainPanel;

    private Map<String, JComponent> currentInputCollection = new ConcurrentHashMap<>();
    private Map<String, String> currentNeededParameters = new ConcurrentHashMap<>();
    private List<ParamMetadata> params = null;
    private RuleType selectedRuleType;
    private ComRule createdRule;
    private ArrayList<Integer> clientsIdGui = new ArrayList<>();

    //Порядковый номер в выпадашке: номер клиента -- правило. Потому что многие ко многим.
    private HashMap<Integer, Map<Integer, ComRule>> guiPairRuleAndClients = new HashMap<>();

    public RuleManagmentDialog(MyProperties prop, AnyPoolService anyPoolService) {
        System.out.println("Run");
        if (anyPoolService == null) {
            log.warn("В конструктор RuleManagmentDialog передан null anyPoolService");
        }
        System.out.println("2");
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
        //Обновление
        refreshGui();

        BT_SelectTemplate.addActionListener(e -> {
            JP_Arguments.removeAll();
            JP_Arguments.revalidate();
            JP_Arguments.repaint();

            params = null;
            selectedRuleType = (RuleType) JC_SampleRules.getSelectedItem();
            log.info("Selected rule: " + selectedRuleType);
            params = ParamMetadata.fromClass(selectedRuleType.getRuleClass());


            // Получаем метаданные для выбранного типа правила
            if (params == null) {
                log.error("Для выбранного класса не найдены метаданные");
                return;
            }

            // Создаем поля ввода для каждого параметра
            JP_Arguments.setLayout(new BoxLayout(JP_Arguments, BoxLayout.PAGE_AXIS));
            currentInputCollection.clear();

            for (ParamMetadata param : params) {
                JLabel label = new JLabel(param.getName() + ":");

                JComponent inputField = createInputField(param);
                inputField.setName(param.getName());
                JP_Arguments.add(label);
                JP_Arguments.add(inputField);
                System.out.println("inputField: " + inputField.getName());
                currentInputCollection.put(param.getName(), inputField);
                log.info("Создано поле ввода для параметра " + param.getName());

            }
            super.repaint();

        });

        BT_CreateRule.addActionListener(e -> {
            log.info("Начинаю создание правила на основе параметров из GUI");
            if (params == null) {
                log.warn("Нет списка парметров для считывания из GUI");
                return;
            }
            currentNeededParameters.clear();
            for (ParamMetadata param : params) {
                if (currentInputCollection.containsKey(param.getName())) {
                    JComponent currentInput = currentInputCollection.get(param.getName());
                    log.info("For parameter: " + param.getName() + " typed like: " + param.getType() + "value: " + getComponentValue(currentInput, param));

                    currentNeededParameters.put(param.getName(), getComponentValue(currentInput, param));
                } else {
                    log.warn("Для параметра " + param.getName() + " не было найдено поле ввода");
                }
            }
            log.info("Завершил сбор параметров. Размер коллекции  " + currentNeededParameters.size());
            //RuleFactory ruleFactory = new RuleFactory();
            try {
                createdRule = RuleFactory.createRule(selectedRuleType.getRuleClass(), currentNeededParameters);
            } catch (InvocationTargetException ex) {
                log.error("InvocationTargetException" + ex.getMessage());
                //throw new RuntimeException(ex);
            } catch (InstantiationException ex) {
                log.error("InstantiationException" + ex.getMessage());
                //throw new RuntimeException(ex);
            } catch (IllegalAccessException ex) {
                log.error("IllegalAccessException" + ex.getMessage());
                //throw new RuntimeException(ex);
            }

            if (createdRule == null) {
                log.error("Empty rule");
                return;
            } else {
                log.info("Создано правило");
                log.info(createdRule.getRuleId());
                log.info(createdRule.getDescription());
                log.info(createdRule.getRuleType());
            }
            log.info("Сохраняю правило в коллекцию");
            ruleStorage.addRule(createdRule);
            refreshGui();
            log.info("Закончил обновление GUI полсе создания правила");
        });

        BT_AddPair.addActionListener(e -> {
            RuleStorage.getInstance().registryRule(clientsIdGui.get(JC_Clients.getSelectedIndex()), createdRule);
            refreshGui();
        });
    }

    private void updateRuleSampleList() {
        log.info("Вызвано обновление списка шаблонов правил");
        JC_SampleRules.removeAllItems();
        for (RuleType value : RuleType.values()) {
            JC_SampleRules.addItem(value);
        }
    }

    private void updateClientsList() {
        log.info("Вызвано обновление списка клиентов");

        JC_Clients.removeAllItems();
        ConcurrentLinkedQueue<Integer> clientsId = AnswerStorage.getClientsList();

        System.out.println(clientsId.size());
        int count = 0;
        for (Integer id : clientsId) {
            JC_Clients.addItem("dev" + (count + 1) + " (id:" + id + ")");
            clientsIdGui.add(id);
            count++;
        }

    }

    private void updateRuleList() {
        log.info("Вызвано обновление списка правил");
        JC_Rules.removeAllItems();
        List<ComRule> rules = ruleStorage.getAllRules();
        log.info("Коллекция содержит " + rules.size() + " правил");
        int tt = 0;
        for (ComRule rule : rules) {
            JC_Rules.addItem("Trololo " + tt + " ruleId: " + rule.getRuleId());
            tt++;
        }
    }

    private void updateClientRulePairs() {
        log.info("Вызвано обновление списка ассоциаций (клиент/правило)");
        sb.setLength(0);


        ConcurrentMap<Integer, List<ComRule>> pairs = RuleStorage.getInstance().getAllBounds();
        log.info("Количество клиентов в хранилище " + pairs.size());
        int count = 0;
        for (Integer i : pairs.keySet()) {
            List<ComRule> rules = pairs.get(i);
            for (ComRule rule : rules) {
                sb.setLength(0);
                sb.append(i).
                        append(" приписан правилу ").
                        append(rule.getRuleId()).
                        append(")\r\n");
                JC_ExistPair.addItem(sb.toString());
                Map<Integer, ComRule> pairForClient = new HashMap<>();
                pairForClient.put(i, rule);
                guiPairRuleAndClients.put(count, pairForClient);
                count++;
            }
        }

        for (Integer i : guiPairRuleAndClients.keySet()) {
            Set<Integer> currentClientIdSet = guiPairRuleAndClients.get(i).keySet();
            Integer currentClientId = currentClientIdSet.iterator().next();
            JC_ExistPair.addItem(
                    i +
                    " Клиент " +
                    currentClientId +
                    " приписан правилу " +
                    guiPairRuleAndClients.get(i).get(currentClientId).getRuleId() +
                    " (" +
                        guiPairRuleAndClients.get(i).get(currentClientId).getRuleType() +
                    ") "
            );
        }

    }

    private void refreshGui() {
        updateRuleSampleList();
        updateRuleList();
        updateClientsList();
        updateClientRulePairs();
        repaint();
    }




    private JComponent createInputField(ParamMetadata param) {
        if (param == null) {
            log.warn("В методе createInputField передан null param");
            return null;
        }

        if (param.getName() == null || param.getName().isEmpty()) {
            log.warn("Пустое имя параметра");
            return null;
        }
        log.warn("Add for " + param.getName() + " typed " + param.getType());
        switch (param.getType().toUpperCase()) {
            case "FLOAT":
                JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                        0.1, param.getMin(), param.getMax(), 0.1
                ));
                return spinner;

            case "INT":
                return new JTextField(10);

            case "BOOLEAN":
                return new JCheckBox();

            case "ENUM":
                return new JComboBox<>(param.getOptions());

            default:
                return new JTextField(20);
        }
    }

    private String getComponentValue(JComponent component, ParamMetadata param) {
        if (component instanceof JSpinner) {
            return ((JSpinner) component).getValue().toString();
        } else if (component instanceof JCheckBox) {
            return ((JCheckBox) component).isSelected() ? "true" : "false";
        } else if (component instanceof JComboBox) {
            return ((JComboBox<?>) component).getSelectedItem().toString();
        } else if (component instanceof JTextField) {
            String text = ((JTextField) component).getText();
            return convertTextToType(text, param.getJavaType());
        }
        return null;
    }

    private String convertTextToType(String text, Class<?> type) {
        if (type == String.class) return text;
        if (type == int.class || type == Integer.class)
            return String.valueOf(Integer.parseInt(text));//проверка что введено верно
        if (type == float.class || type == Float.class) return String.valueOf(Float.parseFloat(text));
        if (type == double.class || type == Double.class) return String.valueOf(Double.parseDouble(text));
        if (type == boolean.class || type == Boolean.class) return String.valueOf(Boolean.parseBoolean(text));
        return text;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        JP_RuleCreate = new JPanel();
        JP_RuleCreate.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(JP_RuleCreate, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        JC_SampleRules = new JComboBox();
        JP_RuleCreate.add(JC_SampleRules, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_SelectTemplate = new JButton();
        BT_SelectTemplate.setText("Select");
        JP_RuleCreate.add(BT_SelectTemplate, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        JP_Arguments = new JPanel();
        JP_Arguments.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        JP_RuleCreate.add(JP_Arguments, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_CreateRule = new JButton();
        BT_CreateRule.setText("CreateRule");
        JP_RuleCreate.add(BT_CreateRule, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        JP_RuleAssociate = new JPanel();
        JP_RuleAssociate.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(JP_RuleAssociate, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_Refresh = new JButton();
        BT_Refresh.setText("Refresh");
        JP_RuleAssociate.add(BT_Refresh, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        JC_Clients = new JComboBox();
        JP_RuleAssociate.add(JC_Clients, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        JC_Rules = new JComboBox();
        JP_RuleAssociate.add(JC_Rules, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_AddPair = new JButton();
        BT_AddPair.setText("AddPair");
        JP_RuleAssociate.add(BT_AddPair, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        JP_ExistingRules = new JPanel();
        JP_ExistingRules.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(JP_ExistingRules, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        JC_ExistPair = new JComboBox();
        JP_ExistingRules.add(JC_ExistPair, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_RemovePair = new JButton();
        BT_RemovePair.setText("RemovePair");
        JP_ExistingRules.add(BT_RemovePair, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
