package com.example.moviltaller3.model

class UserPOJO {
    var uid = ""
    var name: String = ""
    var email: String = ""
    var surname: String = ""
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var status: String = UserStatus.DISCONNECTED.status
    var photoUrl: String = ""
}