package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.HighlightCodeBlock
import me.rerere.rikkahub.ui.components.MarkdownBlock
import me.rerere.rikkahub.ui.components.chat.ChatInput
import me.rerere.rikkahub.ui.components.icons.ListTree
import me.rerere.rikkahub.ui.components.icons.MessageCirclePlus
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.heroAnimation
import org.koin.androidx.compose.koinViewModel

val content = """
# 设置
## 这是二级标题
### 这是三级标题
#### 这是四级标题

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

这里是设置页面的内容

文本**加粗**, *斜体*，~~删除线~~，`代码`

This is inline math $\frac{1}{2}x^2 + \frac{1}{3}y^3$

and block math:   

$$
 f(x) = 2x+4
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
  
this is inline code `def helloWorld()`
  
```javascript
function helloWorld() {
    console.log("Hello, World!");
}
```

```python
def hello_world():
    print("Hello, World!")
```
""".trimIndent()

val formula = """
\f\relax{x} = \int_{-\infty}^\infty
    \f\hat\xi\,e^{2 \pi i \xi x}
    \,d\xi
""".trimIndent()

@Composable
fun ChatPage(vm: ChatVM = koinViewModel()) {
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = {}) {
                        Text("DeepSeek R1")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(MessageCirclePlus, "New Message")
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(ListTree, "Messages")
                    }
                }
            )
        },
        bottomBar = {
            ChatInput()
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


            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    HighlightCodeBlock(
                        """
                        fun helloWorld() {
                            console.log("Hello, World!");
                        }
                    """.trimIndent(), "java"
                    )
                }

                item {
                    Card {
                        MarkdownBlock(content, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}