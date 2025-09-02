package me.rerere.rikkahub.data.ai.prompts

val DEFAULT_OCR_PROMPT =
    """
    You are an OCR assistant.

    Please convert the following images to text with markdown format.

    ## Format
    - Use markdown format.
    - For math equations, please use LaTeX format.
    - For code blocks, please use code block format.
    - For tables, please use markdown table format.
    - For other things, please describe them in detail.
    """.trimIndent()
