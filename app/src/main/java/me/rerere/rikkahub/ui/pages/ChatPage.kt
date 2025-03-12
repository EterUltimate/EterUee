package me.rerere.rikkahub.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.MarkdownBlock
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.heroAnimation

@Composable
fun ChatPage() {
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "设置")
                },
                actions = {

                },
                navigationIcon = {

                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Card(
                modifier = Modifier.heroAnimation("setting_card"),
                onClick = {
                    navController.navigate("setting")
                }
            ) {
                Box(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("设置")
                }
            }

            LazyColumn {
                item {
                    MarkdownBlock(
                        """
                    # 设置
                    ## 这是二级标题
                    ### 这是三级标题
                    #### 这是四级标题
                    
                    这里是设置页面的内容
                    
                    文本**加粗**, *斜体*，~~删除线~~，`代码`
                    
                    ```js
                    console.log("Hello, World!");
                    ```
                    
                    $$
                    f(x) = x^2
                    $$
                    
                    | 标题1 | 标题2 | 标题3 | 
                    | --- | --- | --- |
                    | 内容1 | 内容2 | 内容3 |
                    | 内容1 | 内容2 | 内容3 |
                    
                    > 这是一个引用
                    
                    [这是一个链接](https://www.google.com)
                    
                    - 列表1
                    - 列表2
                    - 列表3
                    
                    1. 列表1
                    2. 列表2
                    3. 列表3
                    
                    - [ ] 任务1
                    - [x] 任务2
                    - [ ] 任务3
                    
                    - [ ] 任务1
                      - [x] 子任务1
                      - [ ] 子任务2
                    - [ ] 任务2
                    - [ ] 任务3
                    
                    - [ ] 任务1
                      - [x]子任务1
                """.trimIndent()
                    )
                }
            }
        }
    }
}