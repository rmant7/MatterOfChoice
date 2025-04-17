// Language code mapping
const languageCodeMap = {
    English: "en",
    Spanish: "es",
    French: "fr",
    "Mandarin Chinese": "zh",
    Hindi: "hi",
    Arabic: "ar",
    Bengali: "bn",
    Russian: "ru",
    Portuguese: "pt",
    Urdu: "ur",
    German: "de",
    Japanese: "ja",
    Swahili: "sw",
    Marathi: "mr",
    Telugu: "te",
    Tamil: "ta",
    Vietnamese: "vi",
    Korean: "ko",
    Turkish: "tr",
  }
  
  // List of all supported language codes
  const supportedLanguages = Object.values(languageCodeMap)
  
  // Cache for loaded translations
  const translationsCache = {}
  
  // Current language
  let currentLanguage = "en"
  
  // Function to load a language file
  async function loadLanguageFile(lang) {
    if (translationsCache[lang]) {
      return translationsCache[lang]
    }
    
    try {
      // Fetch the language file
      const response = await fetch(`/static/js/translations/${lang}.js`)
      
      if (!response.ok) {
        throw new Error(`Failed to load language file: ${response.status}`)
      }
      
      // Get the text content and extract the JSON
      const text = await response.text()
      // Extract the JSON part from "export default {...}"
      const jsonStr = text.replace('export default', '').trim().replace(/;$/, '')
      const translations = JSON.parse(jsonStr)
      
      translationsCache[lang] = translations
      return translations
    } catch (error) {
      console.warn(`Failed to load language file for ${lang}. Falling back to English.`, error)
      return translationsCache.en || {}
    }
  }
  
  // Function to update the UI language
  async function updateLanguage(lang) {
    // Default to English if the language is not supported
    if (!supportedLanguages.includes(lang)) {
      console.warn(`Language ${lang} not supported. Falling back to English.`)
      lang = "en"
    }
  
    try {
      // Ensure English is loaded as fallback
      if (!translationsCache.en) {
        await loadLanguageFile('en')
      }
      
      // Load the language file
      const translations = await loadLanguageFile(lang)
      
      // Update current language
      currentLanguage = lang
      document.documentElement.lang = lang // Update HTML lang attribute
      
      // Update all elements with data-translate attribute
      document.querySelectorAll("[data-translate]").forEach((element) => {
        const key = element.getAttribute("data-translate")
        if (translations[key]) {
          element.textContent = translations[key]
        } else if (translationsCache.en[key]) {
          // Fallback to English if the key is missing in the current language
          element.textContent = translationsCache.en[key]
        }
      })
      
      // Update placeholders
      document.querySelectorAll("[data-translate-placeholder]").forEach((element) => {
        const key = element.getAttribute("data-translate-placeholder")
        if (translations[key]) {
          element.placeholder = translations[key]
        } else if (translationsCache.en[key]) {
          // Fallback to English if the key is missing in the current language
          element.placeholder = translationsCache.en[key]
        }
      })
      
      // Update options in select elements
      document.querySelectorAll("select").forEach((select) => {
        Array.from(select.options).forEach((option) => {
          const translateKey = option.getAttribute("data-translate")
          if (translateKey) {
            if (translations[translateKey]) {
              option.textContent = translations[translateKey]
            } else if (translationsCache.en[translateKey]) {
              // Fallback to English if the key is missing in the current language
              option.textContent = translationsCache.en[translateKey]
            }
          }
        })
      })
    } catch (error) {
      console.error(`Error updating language to ${lang}:`, error)
    }
  }
  
  // Function to get translation for a key
  function getTranslation(key) {
    const translations = translationsCache[currentLanguage]
    
    if (translations && translations[key]) {
      return translations[key]
    }
    
    // Fallback to English
    return (translationsCache.en && translationsCache.en[key]) || key
  }
  
  // Preload English translations to ensure they're always available
  loadLanguageFile('en').then(() => {
    console.log('English translations loaded successfully')
  })
  
  // Event listener for language changes
  document.addEventListener("DOMContentLoaded", () => {
    const languageSelect = document.getElementById("language")
    if (languageSelect) {
      languageSelect.addEventListener("change", (e) => {
        const selectedLanguage = e.target.value
        const langCode = languageCodeMap[selectedLanguage] || "en"
        updateLanguage(langCode)
      })
    }
    
    // Initialize with default language
    updateLanguage(currentLanguage)
  })
  
  // Expose the functions globally
  window.i18n = {
    updateLanguage,
    getTranslation,
    getCurrentLanguage: () => currentLanguage,
  }