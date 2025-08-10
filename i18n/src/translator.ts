import { google } from '@ai-sdk/google';
import { openai } from '@ai-sdk/openai';
import { generateText } from 'ai';
import { StringResource } from './xml-parser';
import { I18nConfig } from './config';
import * as fs from 'fs';
import * as path from 'path';

export interface TranslationProgress {
  current: number;
  total: number;
  key: string;
  status: 'translating' | 'completed' | 'error';
  error?: string;
}

export type ProgressCallback = (progress: TranslationProgress) => void;

// Logging utility
const LOG_FILE = path.join(process.cwd(), 'logs.txt');

function logToFile(message: string): void {
  const timestamp = new Date().toISOString();
  const logEntry = `[${timestamp}] ${message}\n`;
  try {
    fs.appendFileSync(LOG_FILE, logEntry, 'utf8');
  } catch (error) {
    console.error('Failed to write to log file:', error);
  }
}

const LANGUAGE_NAMES: Record<string, string> = {
  'zh': 'Simplified Chinese (简体中文)',
  'zh-rTW': 'Traditional Chinese (繁體中文)',
  'ja': 'Japanese (日本語)',
  'ko': 'Korean (한국어)',
  'es': 'Spanish (Español)',
  'fr': 'French (Français)',
  'de': 'German (Deutsch)',
  'it': 'Italian (Italiano)',
  'pt': 'Portuguese (Português)',
  'ru': 'Russian (Русский)',
};

function getLanguageName(locale: string): string {
  return LANGUAGE_NAMES[locale] || locale;
}

function getModel(config: I18nConfig) {
  switch (config.provider.type.toLowerCase()) {
    case 'google':
    case 'gemini':
      return google(config.provider.model);
    case 'openai':
      return openai(config.provider.model);
    default:
      throw new Error(`Unsupported provider: ${config.provider.type}`);
  }
}

export async function translateString(
  text: string,
  targetLocale: string,
  config: I18nConfig,
  context?: string
): Promise<string> {
  logToFile(`Starting translation - Target: ${targetLocale}, Text: "${text}"`);
  
  try {
    const model = getModel(config);
    const targetLanguage = getLanguageName(targetLocale);

    const prompt = `Translate the following Android app string resource to ${targetLanguage}.

Context: This is a string resource from an Android LLM chat client app called RikkaHub.
${context ? `Additional context: ${context}` : ''}

Original text: "${text}"

Rules:
1. Keep Android string formatting like %1$d, %1$s, \\n, \\', etc. unchanged
2. Preserve XML entities like &amp;, &lt;, &gt;
3. Maintain the natural flow and meaning appropriate for the target language
4. For UI elements, use terms commonly used in mobile apps in that language
5. Return ONLY the translated text, no explanations or quotes

Translation:`;

    logToFile(`Using provider: ${config.provider.type}, model: ${config.provider.model}`);

    const result = await generateText({
      model,
      prompt,
      temperature: 0.3,
    });

    const translatedText = result.text.trim();
    logToFile(`Translation completed - Result: "${translatedText}"`);

    return translatedText;
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    logToFile(`Translation failed - Error: ${errorMessage}`);
    throw error;
  }
}

export async function batchTranslate(
  strings: StringResource[],
  targetLocale: string,
  config: I18nConfig,
  onProgress?: ProgressCallback
): Promise<StringResource[]> {
  logToFile(`Starting batch translation - Target: ${targetLocale}, Total strings: ${strings.length}`);
  
  const results: StringResource[] = [];
  const total = strings.length;
  let successCount = 0;
  let errorCount = 0;

  for (let i = 0; i < strings.length; i++) {
    const stringResource = strings[i];

    try {
      logToFile(`Processing string ${i + 1}/${total} - Key: ${stringResource.key}`);
      
      onProgress?.({
        current: i + 1,
        total,
        key: stringResource.key,
        status: 'translating'
      });

      const translatedValue = await translateString(
        stringResource.value,
        targetLocale,
        config,
        `Key: ${stringResource.key}`
      );

      results.push({
        key: stringResource.key,
        value: translatedValue,
        translatable: stringResource.translatable
      });

      successCount++;
      logToFile(`Successfully translated key: ${stringResource.key}`);

      onProgress?.({
        current: i + 1,
        total,
        key: stringResource.key,
        status: 'completed'
      });

      // Small delay to avoid rate limits
      await new Promise(resolve => setTimeout(resolve, 100));

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      errorCount++;
      logToFile(`Failed to translate key: ${stringResource.key} - Error: ${errorMessage}`);

      onProgress?.({
        current: i + 1,
        total,
        key: stringResource.key,
        status: 'error',
        error: errorMessage
      });

      // For errors, keep the original text as fallback
      results.push({
        key: stringResource.key,
        value: stringResource.value,
        translatable: stringResource.translatable
      });
    }
  }

  logToFile(`Batch translation completed - Total: ${total}, Success: ${successCount}, Errors: ${errorCount}`);
  return results;
}
