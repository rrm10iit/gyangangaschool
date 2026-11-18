package com.gyanganga.school.model;

public final class Enums {
    private Enums() {}

    public enum Role { ADMIN, TEACHER, ACCOUNTANT }

    public enum Frequency { MONTHLY, QUARTERLY, ANNUAL, ONCE }

    public enum PayMode { CASH, UPI, CARD, BANK, CHEQUE }

    public enum StudentStatus { ACTIVE, LEFT }

    public enum SalaryStatus { PENDING, PAID }

    public enum EnquiryStatus { NEW, CONTACTED, CLOSED }

    public enum SettingKind { TEXT, LONG, IMAGE, URL }
}
