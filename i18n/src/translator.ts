import { google } from '@ai-sdk/google';
import { openai } from '@ai-sdk/openai';
import { generateText } from 'ai';
import { StringResource } from './xml-parser';
import { I18nConfig } from './config';

export interface TranslationProgress {
  current: number;
  total: number;
  key: string;
  status: 'translating' | 'completed' | 'error';
  error?: string;
}

export type ProgressCallback = (progress: TranslationProgress) => void;

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

  const result = await generateText({
    model,
    prompt,
    temperature: 0.3,
  });

  return result.text.trim();
}

export async function batchTranslate(
  strings: StringResource[],
  targetLocale: string,
  config: I18nConfig,
  onProgress?: ProgressCallback
): Promise<StringResource[]> {
  const results: StringResource[] = [];
  const total = strings.length;
  
  for (let i = 0; i < strings.length; i++) {
    const stringResource = strings[i];
    
    try {
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
  
  return results;
}