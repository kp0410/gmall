package com.atguigu.gmall.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;


@Data
@NoArgsConstructor
public class BaseAttrInfo implements Serializable{

    @Id
    @Column
    private String id;

    @Column
    private String attrName;

    @Column
    private String catalog3Id;
}
