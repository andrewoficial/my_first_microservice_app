package org.example.web.repository;

import org.example.web.entity.StateMeasure;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/*
Набор готовых запросов для б.д
ToDo
Запрос последних показаний
Запрос пользователей системы
 */
public interface StateMeasureRepository extends CrudRepository<StateMeasure, String> {

    //Нет смысловой нагрузки в этом запросе. Сделать выборку по дате
    @Query(nativeQuery = true, value = "SELECT * FROM tb_measure WHERE device = 1")
    List<StateMeasure> findActive();


    @Query(nativeQuery = true, value = "SELECT * FROM tb_measure WHERE date_time = :arg")
    static boolean existsById(Long arg) {
        return false;
    }


}
