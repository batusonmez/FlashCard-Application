package com.example.flashcardapplication.models

import java.io.Serializable

class User : Serializable{
    private var uid: String? = null
    private var avatar: String? = null
    private var name: String? = null
    private var email: String? = null
    constructor(uid: String?, avatar: String?, name: String?, email: String?) {
        this.uid = uid
        this.avatar = avatar
        this.name = name
        this.email = email
    }
    fun getUid(): String? {
        return uid
    }
    fun setUid(uid: String?) {
        this.uid = uid
    }
    fun getAvatar(): String? {
        return avatar
    }
    fun setAvatar(avatar: String?) {
        this.avatar = avatar
    }
    fun getName(): String? {
        return name
    }
    fun setName(name: String?) {
        this.name = name
    }
    fun getEmail(): String? {
        return email
    }
    fun setEmail(email: String?) {
        this.email = email
    }

    override fun toString(): String {
        return "User{" +
                "uid='" + uid + '\'' +
                ", avatar='" + avatar + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}'
    }
}