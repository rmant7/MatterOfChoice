package com.matterofchoice.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.matterofchoice.MainActivity
import com.matterofchoice.R
import com.matterofchoice.Screens
import com.matterofchoice.common.DropDownMenu
import com.matterofchoice.common.GameButton
import com.matterofchoice.common.GameTextField
import com.matterofchoice.utils.LanguageDefinition
import com.matterofchoice.utils.LanguagePreferenceHelper
import com.matterofchoice.utils.LocaleHelper
import com.matterofchoice.utils.extraLanguages

object PrefKeys {
    const val MY_PREFS = "MyPrefs"
    const val FIRST_OPEN = "firstOpen"
    const val IS_FIRST = "isFirst"
    const val USER_SUBJECT = "userSubject"
    const val USER_AGE = "userAge"
    const val USER_GENDER = "userGender"
    const val USER_QUESTION_TYPE = "userQuestionType"
    const val USER_SUBTYPE = "subtype"
    const val USER_DIFFICULTY = "difficulty"
}

@Composable
fun Settings(navController: NavController) {
    UserInput(navController = navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInput(
    navController: NavController,
) {
    val context = LocalContext.current

    val sharedPreferences = context.getSharedPreferences(PrefKeys.MY_PREFS, Context.MODE_PRIVATE)

    var userSubject by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }

    val questionTypeStudy = stringResource(id = R.string.question_type_study)
    val questionTypeBehavioral = stringResource(id = R.string.question_type_behavioral)
    val questionTypeHiring = stringResource(id = R.string.question_type_hiring)
    val questionTypes = listOf(questionTypeStudy, questionTypeBehavioral, questionTypeHiring)
    val isExposedType = remember { mutableStateOf(false) }

    val subtypesBehavioral = stringArrayResource(id = R.array.subtypes_behavioral_array).toList()
    val subtypesStudy = stringArrayResource(id = R.array.subtypes_study_array).toList()
    val subtypesHiring = stringArrayResource(id = R.array.subtypes_hiring_array).toList()

    val subtypesMap = mapOf(
        questionTypeBehavioral to subtypesBehavioral,
        questionTypeStudy to subtypesStudy,
        questionTypeHiring to subtypesHiring
    )
    val isExposedSub = remember { mutableStateOf(false) }

    val difficults = stringArrayResource(id = R.array.difficulty_levels_array).toList()
    val isDifficultExposed = remember { mutableStateOf(false) }
    val difficult = remember { mutableStateOf(difficults.firstOrNull() ?: "") }

    val userQuestionType = remember { mutableStateOf(questionTypes.firstOrNull() ?: "") }

    val availableSubtypes = subtypesMap[userQuestionType.value] ?: emptyList()
    val subtype = remember { mutableStateOf(availableSubtypes.firstOrNull() ?: "") }

    LaunchedEffect(userQuestionType.value) {
        val updatedSubtypes = subtypesMap[userQuestionType.value] ?: emptyList()
        subtype.value = updatedSubtypes.firstOrNull() ?: ""
    }

    val genderSelectPrompt = stringResource(id = R.string.gender_select_prompt)
    val genderMale = stringResource(id = R.string.gender_male)
    val genderFemale = stringResource(id = R.string.gender_female)
    val displayGenders = listOf(genderSelectPrompt, genderMale, genderFemale)
    val actualGendersForStorage = listOf("", genderMale, genderFemale)
    val userGender = remember { mutableStateOf(actualGendersForStorage[0]) }
    val isExposedGender = remember { mutableStateOf(false) }


    val currentSelectedLanguageCode = remember {
        mutableStateOf(LanguagePreferenceHelper.getSelectedLanguage(context.applicationContext))
    }


    val currentLanguageDisplayName = remember(currentSelectedLanguageCode.value, Unit) {
        val selectedCode = currentSelectedLanguageCode.value
        val displayNamesFromResources = context.resources.getStringArray(R.array.languages)
        val codesFromResources = context.resources.getStringArray(R.array.language_codes)

        // 1. Handle System Default separately (as you did)
        if (selectedCode == LanguagePreferenceHelper.SYSTEM_DEFAULT_MARKER_CODE) {
            // Try to find "System Default" in your resource arrays first
            val systemDefaultIndexInResources =
                codesFromResources.indexOf(LanguagePreferenceHelper.SYSTEM_DEFAULT_MARKER_CODE)
            if (systemDefaultIndexInResources != -1 && systemDefaultIndexInResources < displayNamesFromResources.size) {
                displayNamesFromResources[systemDefaultIndexInResources]
            } else {
                // Fallback: Check if "System Default" is in extraLanguages (if you added it there)
                val systemDefaultInExtra = extraLanguages.find { it.code == LanguagePreferenceHelper.SYSTEM_DEFAULT_MARKER_CODE }
                systemDefaultInExtra?.displayName ?: "System Default" // Use a dedicated string
            }
        } else {
            // 2. Try to find in resource arrays
            val indexInResources = codesFromResources.indexOf(selectedCode)
            if (indexInResources != -1 && indexInResources < displayNamesFromResources.size) {
                displayNamesFromResources[indexInResources]
            } else {
                // 3. If not in resources, try to find in extraLanguages
                val languageInExtra = extraLanguages.find { it.code == selectedCode }
                if (languageInExtra != null) {
                    languageInExtra.displayName
                } else {
                    // 4. Fallback: Try to find the default app language in resources (as you did)
                    val defaultAppCodeIndexInResources =
                        codesFromResources.indexOf(LanguagePreferenceHelper.DEFAULT_APP_LANGUAGE_CODE)
                    if (defaultAppCodeIndexInResources != -1 && defaultAppCodeIndexInResources < displayNamesFromResources.size) {
                        displayNamesFromResources[defaultAppCodeIndexInResources]
                    } else {
                        // 5. Ultimate Fallback: Check if default app language is in extraLanguages
                        val defaultAppLangInExtra = extraLanguages.find { it.code == LanguagePreferenceHelper.DEFAULT_APP_LANGUAGE_CODE }
                        defaultAppLangInExtra?.displayName ?: context.getString(R.string.settings_hint_select_language)
                    }
                }
            }
        }
    }

    var languageDropdownExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!sharedPreferences.contains(PrefKeys.FIRST_OPEN)) {
            sharedPreferences.edit().putBoolean(PrefKeys.FIRST_OPEN, false).apply()
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = stringResource(id = R.string.settings_title),
                fontSize = 28.sp,
            )
            Text(
                text = stringResource(id = R.string.settings_subtitle),
                fontSize = 12.sp,
            )

            GameTextField(
                text = userSubject,
                onValueChange = { userSubject = it },
                labelTxt = stringResource(id = R.string.settings_label_subject)
            )

            DropDownMenu(
                itemsList = questionTypes,
                isExposed = isExposedType,
                selectedItem = userQuestionType,
                hint = stringResource(id = R.string.settings_hint_select_question_type)
            )
            DropDownMenu(
                itemsList = availableSubtypes,
                isExposed = isExposedSub,
                selectedItem = subtype,
                hint = stringResource(id = R.string.settings_hint_select_subtype)
            )
            DropDownMenu(
                itemsList = difficults,
                isExposed = isDifficultExposed,
                selectedItem = difficult,
                hint = stringResource(id = R.string.settings_hint_select_difficulty)
            )
            DropDownMenu(
                itemsList = displayGenders,
                isExposed = isExposedGender,
                selectedItem = userGender,
                hint = stringResource(id = R.string.settings_hint_select_gender_optional)
            )

            GameTextField(
                text = userAge,
                onValueChange = { userAge = it },
                labelTxt = stringResource(id = R.string.settings_label_age)
            )

            Text(
                text = stringResource(id = R.string.settings_hint_select_language),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 4.dp)
                    .align(Alignment.Start)
            )
            ExposedDropdownMenuBox(
                expanded = languageDropdownExpanded,
                onExpandedChange = { languageDropdownExpanded = !languageDropdownExpanded },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = currentLanguageDisplayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.settings_hint_select_language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )


                ExposedDropdownMenu(
                    expanded = languageDropdownExpanded,
                    onDismissRequest = { languageDropdownExpanded = false }
                ) {

                    val resourceDisplayNames = stringArrayResource(id = R.array.languages)
                    val resourceCodes = stringArrayResource(id = R.array.language_codes)

                    val resourceLanguageOptions = resourceDisplayNames.mapIndexedNotNull { index, name ->
                        resourceCodes.getOrNull(index)?.let { code ->
                            LanguageDefinition(name, code)
                        }
                    }.toMutableList()
                    val extraLanguageDefinitions = extraLanguages
                    val allLanguageOptions = (resourceLanguageOptions + extraLanguageDefinitions)
                        .distinctBy { it.code } // Ensure codes are unique if overlap is possible
                        .sortedBy { it.displayName }




                    allLanguageOptions.forEach { langOption ->
                        DropdownMenuItem(
                            text = { Text(langOption.displayName) },
                            onClick = {
                                val codeToSetForLocaleHelper = langOption.code
                                val currentlyPersistedLang =
                                    LanguagePreferenceHelper.getSelectedLanguage(context.applicationContext)

                                if (currentlyPersistedLang != codeToSetForLocaleHelper) {

                                    LocaleHelper.setLocale(
                                        context.applicationContext,
                                        codeToSetForLocaleHelper
                                    )
                                    currentSelectedLanguageCode.value =
                                        LanguagePreferenceHelper.getSelectedLanguage(context.applicationContext)
                                    (context as? MainActivity)?.recreateActivity()
                                }
                                languageDropdownExpanded = false
                            }
                        )
                    }
                }

            }

            GameButton(
                onClick = {
                    // Your existing logic for saving preferences
                    val editor = sharedPreferences.edit() // Get editor here
                    editor.putBoolean(PrefKeys.IS_FIRST, false)
                    editor.putString(PrefKeys.USER_SUBJECT, userSubject).apply()
                    editor.putString(PrefKeys.USER_AGE, userAge).apply()
                    editor.putString(PrefKeys.USER_GENDER, userGender.value).apply()
                    editor.putString(PrefKeys.USER_QUESTION_TYPE, userQuestionType.value).apply()
                    editor.putString(PrefKeys.USER_SUBTYPE, subtype.value).apply()
                    editor.putString(PrefKeys.USER_DIFFICULTY, difficult.value).apply()
                    editor.apply()

                    navController.navigate(Screens.GameScreen.screen) {
                        popUpTo(0) { inclusive = true } // Added inclusive as it's common
                    }
                },
                text = stringResource(id = R.string.settings_button_generate_cases)
            )
        }
    }
}
