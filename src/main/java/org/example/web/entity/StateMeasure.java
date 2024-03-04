package org.example.web.entity;

/*
класс, который по полям превращается в поля базы данных
DIO?
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_measure")
public class StateMeasure {

    @Id
    private Long id;

    @NotNull
    @Size(min = 0, max = 100)
    private Long device;

    @NotNull
    @Column(name = "measured_value")
    private Long value;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDevice() {
        return device;
    }

    public void setDevice(Long device) {
        this.device = device;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}
