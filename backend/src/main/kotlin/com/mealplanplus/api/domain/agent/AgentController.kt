package com.mealplanplus.api.domain.agent

import org.springframework.ai.chat.client.ChatClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/agent")
class AgentController(
    private val chatClientBuilder: ChatClient.Builder,
    private val tools: MealPlanToolService
) {

    private val chatClient: ChatClient by lazy { chatClientBuilder.build() }

    @PostMapping("/chat")
    fun chat(@RequestBody request: AgentChatRequest): AgentChatResponse {
        val today = request.date ?: LocalDate.now().toString()
        val slotHint = request.slot?.let { "The user is likely eating $it." } ?: ""

        val systemPrompt = """
            You are a food logging assistant for MealPlan+, a meal planning app.
            Today's date is $today. $slotHint

            Help the user log their meals. When they describe food they ate:
            1. Call searchFoods to find matching items in the database.
            2. Call logFood with the correct food ID, quantity, unit, slot, and date.
            3. Confirm what you logged with the food name and approximate calories.

            Meal slots: BREAKFAST, MORNING_SNACK, LUNCH, DINNER, EVENING_SNACK.
            Default unit is GRAM unless the user specifies pieces, cups, etc.
            If a food is not found, say so and suggest the user add it manually.
            Keep replies short and friendly.
        """.trimIndent()

        val reply = chatClient.prompt()
            .system(systemPrompt)
            .user(request.message)
            .tools(tools)
            .call()
            .content() ?: "Sorry, I couldn't process that request."

        return AgentChatResponse(reply = reply)
    }
}
