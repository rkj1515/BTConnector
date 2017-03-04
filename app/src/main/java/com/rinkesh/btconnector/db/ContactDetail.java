package com.rinkesh.btconnector.db;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = GeneralDBManager.class)
public class ContactDetail extends BaseModel {

    @Column
    @PrimaryKey(autoincrement = true)
    public int sNo;

    @Column
    public String contactId;

    @Column
    public String name;

    @Column
    public String phoneNumber;

}