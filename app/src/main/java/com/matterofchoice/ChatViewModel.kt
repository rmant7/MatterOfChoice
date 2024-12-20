package com.matterofchoice


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ChatViewModel : ViewModel() {
    private val _messageList = MutableStateFlow<List<MessageModel>>(emptyList())
    val messageList = _messageList.asStateFlow()

    //private val _responseList = MutableStateFlow<>
    //val responseList = _responseList.asStateFlow()


    private lateinit var userGender: String
    private lateinit var userLanguage: String
    private lateinit var userAge: String
    private lateinit var userSubject: String

    val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = Constants.apiKey
    )

    fun getUserInfo(gender: String, language: String, age: String, subject: String) {
        userGender = gender
        userLanguage = language
        userAge = age
        userSubject = subject

    }

    fun sendMessage() {

        viewModelScope.launch {
            //_messageList.value += MessageModel(question, "user")

            try {

                val chat = generativeModel.startChat(
                    history = _messageList.value.map {
                        content(it.role) {
                            text(it.message)
                        }
                    }.toList()
                )

                _messageList.value += MessageModel("Typing....", "model")

                val cases =
                    // let's start with 1 situation
                    "start directly with val scenarios. You are developing scenarios for an educational game for children $userGender , $userAge years old. This game teaches the child how to act in different life situations and make appropriate behavioral decisions." +
                            " Your task is to create a list of 1 different life situations that can occur on $userSubject Do not repeat situations or mention any possible solutions and give 4 choices,  2) Evaluate each option using a 5-point scoring system based on three criteria: health, money, friendship. The ratings can be negative. 3) Determine the optimal choice of action in this situation.The  answer should be provided in Kotlin dictionary format." +
                            " Here is an example answer with various scenarios [\\\"You are walking down the street with your friend and see someone littering on the sidewalk. What will you do?\\\", \\\"You go to a toy store and see the toy you have wanted for a long time, but you don't have enough money to buy it. What will you do?\\\", \\\"You are playing in the sandbox with your friends, building a sandcastle. Another child comes and starts destroying your castle. What will you do?\\\"]\",\n"

                val options =
                    "(You are developing scenarios for an educational game for children $userGender , $userAge years old. This game teaches the child how to deal with challenges in different life situations and make optimal behavioral decisions. Your task: 1) Prepare a list of the child's behavioral options for the situation: $cases. 2) Evaluate each option using a 5-point scoring system based on three criteria: health, money, friendship. The ratings can be negative. 3) Determine the optimal choice of action in this situation. The answer should be provided in kotlin dictionary format. Here is an example of the answer: {case:{case}, options: [{number: 1, option: Option text 1, health: 1, money: 0, friends: 4}, {number: 2, option: Option text 2, health: -2, money: -3, friends: 3}, {number: 3, option: Option text 3, health: 0, money: 5, friends: -5}, {number: 4, option: Option text 4, health: 2, money: 1, friends: 4}, {number: 5, option: Option text 5, health: 0, money: 2, friends: 0}], optimal: 3}"


                val response = chat.sendMessage(cases)
                Log.v("responseChat", response.text.toString())
                response.text?.trimIndent()?.removeRange(0,9)

                Log.v("responseChat", response.text.toString())

                //_responseList.value = response.text?.toList()

                try {
                    // Convert JSON string to HashMap
                    val gson = Gson()
                    val type = object : TypeToken<HashMap<String, String>>() {}.type
                    val parsedMap: HashMap<String, String> = gson.fromJson(response.text, type)

                    // Update state flow
                    //_responseList.emit(parsedMap)
                    Log.v("stateFlow", parsedMap.toString())

                } catch (e: Exception) {
                    Log.e("fetchAndParseResponse", "Error parsing response: ${e.message}")
                }


                _messageList.value = _messageList.value.dropLast(1) + MessageModel(
                    response.text.toString(),
                    "model"
                )

            } catch (e: Exception) {
                _messageList.value = _messageList.value.dropLast(1) + MessageModel(
                    "Error: ${e.message}",
                    "model"
                )

            }

        }

    }
}