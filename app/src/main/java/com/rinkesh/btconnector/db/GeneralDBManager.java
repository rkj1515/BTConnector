package com.rinkesh.btconnector.db;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.rinkesh.btconnector.util.Utils;

import java.util.List;

@Database(name = Utils.GENERAL_DB_NAME, version = 1)
public class GeneralDBManager {

    private static final GeneralDBManager generaldbManager = new GeneralDBManager();

    private GeneralDBManager() {
    }

    public static GeneralDBManager getInstance() {
        return generaldbManager;
    }

    public void deleteBookingListTables() {
        try {
            Delete.tables(ContactDetail.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ContactDetail getContactFromName(String name) {
        if (name == null || name.equals("null")) {
            return null;
        }
        try {
            return new Select().from(ContactDetail.class).where(ContactDetail_Table.name.like("%" + name + "%")).querySingle();
        } catch (Exception e) {
            e.printStackTrace();
            return new ContactDetail();
        }
    }

    public ContactDetail getContactFromNumber(String number) {
        if (number == null || number.equals("null")) {
            return null;
        }
        if (number.length() > 10) {
            number = number.substring(3, number.length());
        }
        try {
            return new Select().from(ContactDetail.class).where(ContactDetail_Table.phoneNumber.like("%" + number + "%")).querySingle();
        } catch (Exception e) {
            e.printStackTrace();
            return new ContactDetail();
        }
    }

    public boolean isContactAvailable(String contact_id) {
        ContactDetail contactDetail = new Select().from(ContactDetail.class).where(ContactDetail_Table.contactId.like(contact_id)).querySingle();
        return contactDetail != null;
    }

    public int getCount() {
        List<ContactDetail> contactDetail = new Select().from(ContactDetail.class).queryList();
        return contactDetail.size();

    }
}