// This script builds non-module versions of the translation files
// It should be run as part of your build process

import fs from 'fs'
import path from 'path'
import { supportedLanguages } from './translations/registry.js'

const TRANSLATIONS_DIR = path.join(__dirname, 'translations')
const OUTPUT_DIR = path.join(__dirname, 'dist')

// Ensure the output directory exists
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

// Convert module files to non-module format
supportedLanguages.forEach(lang => {
  const inputPath = path.join(TRANSLATIONS_DIR, `${lang}.js`)
  const outputPath = path.join(OUTPUT_DIR, `${lang}.js`)
  
  // Skip if the input file doesn't exist
  if (!fs.existsSync(inputPath)) {
    console.log(`Language file for ${lang} doesn't exist. Skipping.`)
    return
  }
  
  // Read the module file
  const moduleContent = fs.readFileSync(inputPath, 'utf8')
  
  // Extract the JSON part from "export default {...}"
  const jsonStr = moduleContent.replace('export default', '').trim().replace(/;$/, '')
  
  // Create a non-module version
  const nonModuleContent = `window.translations = window.translations || {};
window.translations["${lang}"] = ${jsonStr};`
  
  fs.writeFileSync(outputPath, nonModuleContent)
  console.log(`Created non-module version for ${lang}`)
})

// Create the main translations loader
const loaderContent = `// Translations loader
window.loadTranslation = function(lang) {
  return new Promise((resolve, reject) => {
    if (window.translations && window.translations[lang]) {
      resolve(window.translations[lang]);
      return;
    }
    
    const script = document.createElement('script');
    script.src = '/static/js/dist/' + lang + '.js';
    script.onload = function() {
      if (window.translations && window.translations[lang]) {
        resolve(window.translations[lang]);
      } else {
        reject(new Error('Failed to load translation: ' + lang));
      }
    };
    script.onerror = function() {
      reject(new Error('Failed to load translation script: ' + lang));
    };
    document.head.appendChild(script);
  });
};`

fs.writeFileSync(path.join(OUTPUT_DIR, 'translations-loader.js'), loaderContent)
console.log('Created translations loader')

console.log('All non-module translation files built successfully!')