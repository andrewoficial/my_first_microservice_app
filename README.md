# Elephant Monitor

## Описание

Elephant Monitor - это приложение, которое на данный момент больше всего похоже на терминал для COM-порта. 
Целевая идея приложения - система управления большим количеством устройвств, подключенных к множеству com-портов с возможностью визуализации результата выполнения команд, 
возможностью индивидуальной настройки приборов, возможности отображения данных в браузере. 


## Технологии

В данном приложении были использованы следующие технологии и инструменты:

- Java: Основной язык программирования, на котором написано приложение.
- Java swing для реализации нативного пользовательского интерфейса.
- jSerialComm для управления соединениями с com-портами.
- JFree для визуализации результатов выполнения команд.
- JUnit для написания автотестов и обеспечения надежности и корректности работы приложения.
- Log4j для логирования событий и отладки.
- Apache Commons DBCP для управления подключениями к базам данных.
- Spring Data JPA для для реализации Java Persistence API (для сохранения объектов в базе данных)
- Spring Starter Validation для проверки данных, приходящих от пользователя (через web-интерфейс)
- Spring security для реализации авторизации пользователей, получения токенов.
- Spring oauth2 для реализации авторизации по стандарту OAuth 2.0.
- Lombok для автоматической генерации гетеров и сетеров.
- Maven: Использован для управления зависимостями и сборки проекта.



## Запуск приложения
Для успешного запуска приложения Вам понадобиться:
- Java Runtime Environment (JRE) или Java Development Kit (JDK): Java-17. 

- Все зависимости, указанные в файле pom.xml для Maven.

- Необходим файл configAcces.properties в  директории src/main/resources. 

    

## Лицензия

##### CC BY-NC 4.0 в следующей нотации:
  ###### RU
     Creative Commons Attribution-NonCommercial 4.0 Международная общедоступная лицензия
     
     Осуществляя Лицензионные права (определенные ниже), Вы принимаете и соглашаетесь соблюдать положения и условия настоящей публичной лицензии Creative Commons Attribution-NonCommercial 4.0 International ("Публичная лицензия"). В той мере, в какой эта Публичная лицензия может быть истолкована как договор, Вам предоставляются Лицензионные права при условии, что Вы принимаете настоящие положения и условия, а Лицензиар предоставляет Вам такие права с учетом выгод, которые Лицензиар получает от предоставления Лицензируемых материалов. на этих условиях.
    
     Вы можете:
     - Распространять — копируйте и распространяйте материал на любом носителе и в любом формате
     - Адаптировать — изменять, адаптировать и создавать на основе 
     
     На следующих условиях:
     - Авторство — вы должны предоставить ссылку на лицензию и указать, ссылку на репозиторий проекта, были ли внесены изменения. Вы можете сделать это любым разумным способом, но никоим образом не предполагающим, что лицензиар одобряет вас или ваше использование.
     - NonCommercial — Вы не можете использовать материал в коммерческих целях.
     
  ###### EN
    Creative Commons Attribution-NonCommercial 4.0 International Public License
    
    By exercising the Licensed Rights (defined below), You accept and agree to be bound by the terms and conditions of this Creative Commons Attribution-NonCommercial 4.0 International Public License ("Public License"). To the extent this Public License may be interpreted as a contract, You are granted the Licensed Rights in consideration of Your acceptance of these terms and conditions, and the Licensor grants You such rights in consideration of benefits the Licensor receives from making the Licensed Material available under these terms and conditions.
    
    You are free to:
    - Share — copy and redistribute the material in any medium or format
    - Adapt — remix, transform, and build upon the material
    
    Under the following terms:
    - Attribution — You must give appropriate credit, provide a link to the license,link to the github page project and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
    - NonCommercial — You may not use the material for commercial purposes.

## Ответственность
###### RU
    Программный продукт, представленный в этом репозитории, предоставляется "как есть" без каких-либо явных или подразумеваемых гарантий, включая, но не ограничиваясь, подразумеваемыми гарантиями коммерческой ценности, пригодности для конкретной цели и невыполнения прав. 
    Разработчик не несет ответственности за любые проблемы, ошибки или неполадки, возникшие при использовании данного продукта. Использование продукта осуществляется на ваш собственный риск.
      
###### EN
    The software product provided in this repository is provided "as is" without warranty of any kind, either express or implied, including, but not limited to, the implied warranties of merchantability, fitness for a particular purpose, and non-infringement.
    The developer is not responsible for any problems, errors or malfunctions that occur when using this product. Use of the product is at your own risk.

## Обратная связь
Ниже найдете список ссылок для связи с автором.

| Платформа     | Ссылка                                                                    | Отвечу за |
| ------------- |:-------------------------------------------------------------------------:| --------- |
| Почта         | [Ссылка](mailto:andrewoficial@yandex.ru "Ссылка")                         | 24 часа   |
| LinkedIn      | [Ссылка](https://www.linkedin.com/in/andrey-kantser-126554258/ "Ссылка")  | 3 часа    |
| Telegram      | [Ссылка](https://t.me/function_void "Ссылка")                             | 5 минут   |
