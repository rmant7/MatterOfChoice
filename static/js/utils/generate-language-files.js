// This script generates placeholder language files for all supported languages
// It should be run once to create the initial files

import fs from 'fs'
import path from 'path'
import { supportedLanguages } from './translations/registry.js'
import enTranslations from './translations/en.js'

const TRANSLATIONS_DIR = path.join(__dirname, 'translations')

// Ensure the translations directory exists
if (!fs.existsSync(TRANSLATIONS_DIR)) {
  fs.mkdirSync(TRANSLATIONS_DIR, { recursive: true })
}

// Generate placeholder files for all supported languages
supportedLanguages.forEach(lang => {
  const filePath = path.join(TRANSLATIONS_DIR, `${lang}.js`)
  
  // Skip if the file already exists
  if (fs.existsSync(filePath)) {
    console.log(`Language file for ${lang} already exists. Skipping.`)
    return
  }
  
  // Create a placeholder file with English translations
  const fileContent = `export default ${JSON.stringify(enTranslations, null, 2)}`
  
  fs.writeFileSync(filePath, fileContent)
  console.log(`Created placeholder language file for ${lang}`)
})

console.log('All language files generated successfully!')