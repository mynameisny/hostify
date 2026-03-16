package me.ningyu.app.hostify.entity;

public enum EntryType
{
    /** 正常 hosts 条目（IP + 域名） */
    NORMAL,

    /** 注释行（# 开头的纯文字行） */
    COMMENT,

    /** 空行 */
    BLANK
}
