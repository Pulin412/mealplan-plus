package com.mealplanplus.api.domain.log

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "logged_meal_slots",
    uniqueConstraints = [UniqueConstraint(columnNames = ["firebase_uid", "date", "slot"])]
)
class LoggedMealSlot(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val date: LocalDate = LocalDate.now(),
    val slot: String = "",
    var isLogged: Boolean = false
)
