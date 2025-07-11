package org.example.services.rule.com;

import lombok.Getter;
import org.example.device.SomeDevice;

import java.io.Serializable;
import java.util.Map;

public interface ComRule extends Serializable {
    String generateCommand(); // Генерация команды с параметрами
    void processResponse(byte[] response); // Обрабатывает ответ (если нужно)
    void reset(); // Сброс состояния
    String getRuleId(); // Уникальный идентификатор правила
    RuleType getRuleType(); // Тип правила для сериализации
    String getDescription(); // Для отображения в UI
    void updateState(); // Обновляет внутреннее состояние (например, увеличивает счетчик)

    long getNextPoolDelay(); // Возвращает задержку до следующего опроса (в мс)
    boolean isActiveRule(); // Логика меняет состояние системы или просто получение данных?
    SomeDevice getTargetDevice(); // Возвращает устройство, для которого правило (опционально)
    
    
    boolean waitingForAnswer = false;    
    void setWaitingForAnswer(boolean state);
    boolean isWaitingForAnswer();

    boolean isTimeForAction();
}

