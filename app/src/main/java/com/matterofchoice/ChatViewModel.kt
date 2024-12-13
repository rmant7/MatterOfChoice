package com.matterofchoice


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ChatViewModel : ViewModel() {
    private val _messageList = MutableStateFlow<List<MessageModel>>(emptyList())
    val messageList = _messageList.asStateFlow()

    private val userMessages by lazy { mutableListOf<String>()}
    private val modelMessages by lazy { mutableListOf<String>()}


    private lateinit var userGender: String
    private lateinit var userLanguage: String
    private lateinit var userAge: String

    val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = Constants.apiKey
    )
    fun getUserInfo(gender:String, language:String, age:String){
        userGender = gender
        userLanguage = language
        userAge = age

    }

    fun sendMessage(question: String) {
        viewModelScope.launch {
            _messageList.value += MessageModel(question, "user")
            for (i in 1..4) {

                try {

                    val chat = generativeModel.startChat(
                        history = _messageList.value.map {
                            content(it.role) {
                                text(it.message)
                            }
                        }.toList()
                    )

                    _messageList.value += MessageModel("Typing....", "model")

                    val userInfo = "You are developing scenarios for an educational game for children $userGender, ($userAge years old, the language $userLanguage). the question is "
                    val response = chat.sendMessage(userInfo+question)


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
}