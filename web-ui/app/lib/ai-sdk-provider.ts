/**
 * EterUee AI Provider 适配器
 * 将现有的 SSE API 适配为 AI SDK Provider 格式
 */

import type { LanguageModelV1, LanguageModelV1StreamPart } from '@ai-sdk/provider';

interface EterUeeProviderOptions {
  baseUrl?: string;
  apiKey?: string;
}

/**
 * 创建 EterUee Provider
 */
export function createEterUeeProvider(options: EterUeeProviderOptions = {}) {
  const baseUrl = options.baseUrl || '/api';

  /**
   * 创建语言模型
   */
  function createModel(modelId: string): LanguageModelV1 {
    return {
      specificationVersion: 'v1',
      defaultObjectGenerationMode: 'json',
      modelId,
      provider: 'eteruee',

      async doGenerate(options) {
        // 非流式生成 - 调用后端 API
        const response = await fetch(`${baseUrl}/conversations/generate`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {}),
          },
          body: JSON.stringify({
            model: modelId,
            messages: options.prompt,
            temperature: options.temperature,
            maxTokens: options.maxTokens,
          }),
        });

        if (!response.ok) {
          throw new Error(`API request failed: ${response.statusText}`);
        }

        const data = await response.json();

        return {
          text: data.text,
          finishReason: data.finishReason || 'stop',
          usage: data.usage
            ? {
                promptTokens: data.usage.promptTokens ?? 0,
                completionTokens: data.usage.completionTokens ?? 0,
              }
            : {
                promptTokens: 0,
                completionTokens: 0,
              },
          rawCall: { rawPrompt: options.prompt, rawSettings: {} },
        };
      },

      async doStream(options) {
        // 流式生成 - 使用 SSE
        const response = await fetch(`${baseUrl}/conversations/stream`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
            ...(options.headers || {}),
          },
          body: JSON.stringify({
            model: modelId,
            messages: options.prompt,
            temperature: options.temperature,
            maxTokens: options.maxTokens,
          }),
        });

        if (!response.ok) {
          throw new Error(`API request failed: ${response.statusText}`);
        }

        const reader = response.body?.getReader();
        if (!reader) {
          throw new Error('Response body is not readable');
        }

        const decoder = new TextDecoder();
        let buffer = '';

        return {
          stream: new ReadableStream<LanguageModelV1StreamPart>({
            async start(controller) {
              try {
                while (true) {
                  const { done, value } = await reader.read();
                  if (done) break;

                  buffer += decoder.decode(value, { stream: true });
                  const lines = buffer.split('\n');
                  buffer = lines.pop() || '';

                  for (const line of lines) {
                    if (line.startsWith('data: ')) {
                      const data = line.slice(6);
                      try {
                        const chunk = JSON.parse(data);

                        // 转换 SSE 事件为 AI SDK 格式
                        if (chunk.type === 'text-delta') {
                          controller.enqueue({
                            type: 'text-delta',
                            textDelta: chunk.data.text,
                          });
                        } else if (chunk.type === 'tool-call') {
                          controller.enqueue({
                            type: 'tool-call',
                            toolCallType: 'function',
                            toolCallId: chunk.data.toolCallId,
                            toolName: chunk.data.toolName,
                            args: chunk.data.arguments || '{}',
                          });
                        } else if (chunk.type === 'finish') {
                          controller.enqueue({
                            type: 'finish',
                            finishReason: chunk.data.reason || 'stop',
                            usage: chunk.data.usage
                              ? {
                                  promptTokens: chunk.data.usage.promptTokens ?? 0,
                                  completionTokens: chunk.data.usage.completionTokens ?? 0,
                                }
                              : {
                                  promptTokens: 0,
                                  completionTokens: 0,
                                },
                          });
                          controller.close();
                          return;
                        }
                      } catch (e) {
                        console.warn('Failed to parse SSE chunk:', e);
                      }
                    }
                  }
                }
              } catch (error) {
                controller.error(error);
              } finally {
                controller.close();
              }
            },
            cancel() {
              reader.cancel();
            },
          }),
          rawCall: { rawPrompt: options.prompt, rawSettings: {} },
        };
      },
    };
  }

  return {
    /**
     * 获取语言模型实例
     */
    languageModel(modelId: string): LanguageModelV1 {
      return createModel(modelId);
    },

    /**
     * 便捷方法 - 直接返回模型
     */
    chatModel(modelId: string): LanguageModelV1 {
      return createModel(modelId);
    },
  };
}

/**
 * 默认 Provider 实例
 */
export const eterueeProvider = createEterUeeProvider();
